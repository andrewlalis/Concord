package nl.andrewl.concord_server.client;

import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.ServerUsers;
import nl.andrewl.concord_core.msg.types.UserData;
import nl.andrewl.concord_core.msg.types.client_setup.*;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.util.CollectionUtils;
import org.dizitart.no2.Document;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.filters.Filters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The client manager is responsible for managing the set of clients connected
 * to a server.
 */
public class ClientManager {
	private final ConcordServer server;
	private final Map<UUID, ClientThread> clients;
	private final Map<UUID, ClientThread> pendingClients;
	private final NitriteCollection userCollection;
	private final AuthenticationService authService;

	/**
	 * Constructs a new client manager for the given server.
	 * @param server The server that the client manager is for.
	 */
	public ClientManager(ConcordServer server) {
		this.server = server;
		this.clients = new ConcurrentHashMap<>();
		this.pendingClients = new ConcurrentHashMap<>();
		this.userCollection = server.getDb().getCollection("users");
		CollectionUtils.ensureIndexes(this.userCollection, Map.of(
				"id", IndexType.Unique,
				"username", IndexType.Unique,
				"pending", IndexType.NonUnique
		));
		this.authService = new AuthenticationService(server, this.userCollection);
		// Start a daily scheduled removal of expired session tokens.
		server.getScheduledExecutorService().scheduleAtFixedRate(this.authService::removeExpiredSessionTokens, 1, 1, TimeUnit.DAYS);
	}

	/**
	 * Handles an attempt by a new client to register as a user for this server.
	 * If the server is set to automatically accept all new clients, the new
	 * user is registered and the client is sent a {@link RegistrationStatus}
	 * with the {@link RegistrationStatus.Type#ACCEPTED} value, closely followed
	 * by a {@link ServerWelcome} message. Otherwise, the client is sent a
	 * {@link RegistrationStatus.Type#PENDING} response, which indicates that
	 * the client's registration is pending approval. The client can choose to
	 * remain connected and wait for approval, or disconnect and try logging in
	 * later.
	 *
	 * @param registration The client's registration information.
	 * @param clientThread The client thread.
	 * @throws InvalidIdentificationException If the user's registration info is
	 * not valid.
	 */
	public void handleRegistration(ClientRegistration registration, ClientThread clientThread) throws InvalidIdentificationException {
		Document userDoc = this.userCollection.find(Filters.eq("username", registration.username())).firstOrDefault();
		if (userDoc != null) throw new InvalidIdentificationException("Username is taken.");
		if (this.server.getConfig().isAcceptAllNewClients()) {
			var clientData = this.authService.registerNewClient(registration);
			clientThread.sendToClient(new RegistrationStatus(RegistrationStatus.Type.ACCEPTED, null));
			this.initializeClientConnection(clientData, clientThread);
		} else {
			var clientId = this.authService.registerPendingClient(registration);
			this.initializePendingClientConnection(clientId, registration.username(), clientThread);
		}
	}

	/**
	 * Handles an attempt by a new client to login as an existing user to the
	 * server. If the user's credentials are valid, then the following can
	 * result:
	 * <ul>
	 *     <li>If the user's registration is still pending, they will be sent a
	 *     {@link RegistrationStatus.Type#PENDING} response, to indicate that
	 *     their registration is still pending approval.</li>
	 *     <li>For non-pending (normal) users, they will be logged into the
	 *     server and sent a {@link ServerWelcome} message.</li>
	 * </ul>
	 *
	 * @param login The client's login credentials.
	 * @param clientThread The client thread managing the connection.
	 * @throws InvalidIdentificationException If the client's credentials are
	 * incorrect.
	 */
	public void handleLogin(ClientLogin login, ClientThread clientThread) throws InvalidIdentificationException {
		Document userDoc = this.authService.findAndAuthenticateUser(login);
		if (userDoc == null) throw new InvalidIdentificationException("Username or password is incorrect.");
		UUID userId = userDoc.get("id", UUID.class);
		String username = userDoc.get("username", String.class);
		boolean pending = userDoc.get("pending", Boolean.class);
		if (pending) {
			this.initializePendingClientConnection(userId, username, clientThread);
		} else {
			String sessionToken = this.authService.generateSessionToken(userId);
			this.initializeClientConnection(new ClientConnectionData(userId, username, sessionToken, false), clientThread);
		}
	}

	/**
	 * Handles an attempt by a new client to login as an existing user to the
	 * server with a session token from their previous session. If the token is
	 * valid, the user will be logged in and sent a {@link ServerWelcome}
	 * response.
	 *
	 * @param sessionResume The session token data.
	 * @param clientThread The client thread managing the connection.
	 * @throws InvalidIdentificationException If the token is invalid or refers
	 * to a non-existent user.
	 */
	public void handleSessionResume(ClientSessionResume sessionResume, ClientThread clientThread) throws InvalidIdentificationException {
		Document userDoc = this.authService.findAndAuthenticateUser(sessionResume);
		if (userDoc == null) throw new InvalidIdentificationException("Invalid session. Log in to obtain a new session token.");
		UUID userId = userDoc.get("id", UUID.class);
		String username = userDoc.get("username", String.class);
		String sessionToken = this.authService.generateSessionToken(userId);
		this.initializeClientConnection(new ClientConnectionData(userId, username, sessionToken, false), clientThread);
	}

	/**
	 * Used to accept or reject a pending user's registration. If the given user
	 * is not pending approval, this method does nothing.
	 * @param userId The id of the pending user.
	 * @param accepted Whether to accept or reject.
	 * @param reason The reason for rejection (or acceptance). This may be null.
	 */
	public void decidePendingUser(UUID userId, boolean accepted, String reason) {
		Document userDoc = this.userCollection.find(Filters.and(Filters.eq("id", userId), Filters.eq("pending", true))).firstOrDefault();
		if (userDoc != null) {
			if (accepted) {
				userDoc.put("pending", false);
				this.userCollection.update(userDoc);
				// If the pending user is still connected, upgrade them to a normal connected client.
				var clientThread = this.pendingClients.remove(userId);
				if (clientThread != null) {
					clientThread.sendToClient(new RegistrationStatus(RegistrationStatus.Type.ACCEPTED, reason));
					String username = userDoc.get("username", String.class);
					String sessionToken = this.authService.generateSessionToken(userId);
					this.initializeClientConnection(new ClientConnectionData(userId, username, sessionToken, true), clientThread);
				}
			} else {
				this.userCollection.remove(userDoc);
				var clientThread = this.pendingClients.remove(userId);
				if (clientThread != null) {
					clientThread.sendToClient(new RegistrationStatus(RegistrationStatus.Type.REJECTED, reason));
				}
			}
		}
	}

	/**
	 * Standard flow for initializing a connection to a client who has already
	 * sent their identification message, and that has been checked to be valid.
	 * @param clientData The data about the client that has connected.
	 * @param clientThread The thread managing the client's connection.
	 */
	private void initializeClientConnection(ClientConnectionData clientData, ClientThread clientThread) {
		clientThread.setClientId(clientData.id());
		clientThread.setClientNickname(clientData.username());
		var defaultChannel = this.server.getChannelManager().getDefaultChannel().orElseThrow();
		clientThread.sendToClient(new ServerWelcome(clientData.id(), clientData.sessionToken(), defaultChannel.getId(), defaultChannel.getName(), this.server.getMetaData()));
		this.clients.put(clientData.id(), clientThread); // We only add the client after sending the welcome, to make sure that we send the welcome packet first.
		defaultChannel.addClient(clientThread);
		clientThread.setCurrentChannel(defaultChannel);
		this.broadcast(new ServerUsers(this.getConnectedClients().toArray(new UserData[0])));
	}

	/**
	 * Initializes a connection to a client whose registration is pending, thus
	 * they should simply keep their connection alive, and receive a {@link RegistrationStatus.Type#PENDING}
	 * message, instead of a {@link ServerWelcome}.
	 * @param clientId The id of the client.
	 * @param pendingUsername The client's username.
	 * @param clientThread The thread managing the client's connection.
	 */
	private void initializePendingClientConnection(UUID clientId, String pendingUsername, ClientThread clientThread) {
		clientThread.setClientId(clientId);
		clientThread.setClientNickname(pendingUsername);
		clientThread.sendToClient(RegistrationStatus.pending());
		this.pendingClients.put(clientId, clientThread);
	}

	/**
	 * De-registers a client from the server, removing them from any channel
	 * they're currently in.
	 * @param clientId The id of the client to remove.
	 */
	public void handleLogOut(UUID clientId) {
		var pendingClient = this.pendingClients.remove(clientId);
		if (pendingClient != null) {
			pendingClient.shutdown();
		}
		var client = this.clients.remove(clientId);
		if (client != null) {
			client.getCurrentChannel().removeClient(client);
			client.shutdown();
			System.out.println("Client " + client + " has disconnected.");
			this.broadcast(new ServerUsers(this.getConnectedClients().toArray(new UserData[0])));
		}
	}

	/**
	 * Sends a message to every connected client, ignoring any channels. All
	 * clients connected to this server will receive this message.
	 * @param message The message to send.
	 */
	public void broadcast(Message message) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(message.byteSize());
		try {
			this.server.getSerializer().writeMessage(message, baos);
			byte[] data = baos.toByteArray();
			for (var client : this.clients.values()) {
				client.sendToClient(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return The list of connected clients.
	 */
	public List<UserData> getConnectedClients() {
		return this.clients.values().stream()
				.sorted(Comparator.comparing(ClientThread::getClientNickname))
				.map(ClientThread::toData)
				.collect(Collectors.toList());
	}

	/**
	 * @return The list of connected, pending clients.
	 */
	public List<UserData> getPendingClients() {
		return this.pendingClients.values().stream()
				.sorted(Comparator.comparing(ClientThread::getClientNickname))
				.map(ClientThread::toData)
				.collect(Collectors.toList());
	}

	/**
	 * @return The set of ids of all connected clients.
	 */
	public Set<UUID> getConnectedIds() {
		return this.clients.keySet();
	}

	/**
	 * Tries to find a connected client with the given id.
	 * @param id The id to look for.
	 * @return An optional client thread.
	 */
	public Optional<ClientThread> getClientById(UUID id) {
		return Optional.ofNullable(this.clients.get(id));
	}

	/**
	 * Tries to find a pending client with the given id.
	 * @param id The id to look for.
	 * @return An optional client thread.
	 */
	public Optional<ClientThread> getPendingClientById(UUID id) {
		return Optional.ofNullable(this.pendingClients.get(id));
	}
}
