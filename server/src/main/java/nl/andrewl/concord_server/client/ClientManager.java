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

	public void handleRegistration(ClientRegistration registration, ClientThread clientThread) throws InvalidIdentificationException {
		Document userDoc = this.userCollection.find(Filters.eq("username", registration.username())).firstOrDefault();
		if (userDoc != null) throw new InvalidIdentificationException("Username is taken.");
		if (this.server.getConfig().isAcceptAllNewClients()) {
			var clientData = this.authService.registerNewClient(registration);
			this.initializeClientConnection(clientData, clientThread);
		} else {
			var clientId = this.authService.registerPendingClient(registration);
			this.initializePendingClientConnection(clientId, registration.username(), clientThread);
		}
	}

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
			this.initializeClientConnection(new AuthenticationService.ClientConnectionData(userId, username, sessionToken, false), clientThread);
		}
	}

	public void handleSessionResume(ClientSessionResume sessionResume, ClientThread clientThread) throws InvalidIdentificationException {
		Document userDoc = this.authService.findAndAuthenticateUser(sessionResume);
		if (userDoc == null) throw new InvalidIdentificationException("Invalid session. Log in to obtain a new session token.");
		UUID userId = userDoc.get("id", UUID.class);
		String username = userDoc.get("username", String.class);
		String sessionToken = this.authService.generateSessionToken(userId);
		this.initializeClientConnection(new AuthenticationService.ClientConnectionData(userId, username, sessionToken, false), clientThread);
	}

	public void decidePendingUser(UUID userId, boolean accepted) {
		Document userDoc = this.userCollection.find(Filters.and(Filters.eq("id", userId), Filters.eq("pending", true))).firstOrDefault();
		if (userDoc != null) {
			if (accepted) {
				userDoc.put("pending", false);
				this.userCollection.update(userDoc);
				// If the pending user is still connected, upgrade them to a normal connected client.
				var clientThread = this.pendingClients.remove(userId);
				if (clientThread != null) {
					clientThread.sendToClient(new RegistrationStatus(RegistrationStatus.Type.ACCEPTED));
					String username = userDoc.get("username", String.class);
					String sessionToken = this.authService.generateSessionToken(userId);
					this.initializeClientConnection(new AuthenticationService.ClientConnectionData(userId, username, sessionToken, true), clientThread);
				}
			} else {
				this.userCollection.remove(userDoc);
				var clientThread = this.pendingClients.remove(userId);
				if (clientThread != null) {
					clientThread.sendToClient(new RegistrationStatus(RegistrationStatus.Type.REJECTED));
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
	private void initializeClientConnection(AuthenticationService.ClientConnectionData clientData, ClientThread clientThread) {
		this.clients.put(clientData.id(), clientThread);
		clientThread.setClientId(clientData.id());
		clientThread.setClientNickname(clientData.nickname());
		var defaultChannel = this.server.getChannelManager().getDefaultChannel().orElseThrow();
		clientThread.sendToClient(new ServerWelcome(clientData.id(), clientData.sessionToken(), defaultChannel.getId(), defaultChannel.getName(), this.server.getMetaData()));
		defaultChannel.addClient(clientThread);
		clientThread.setCurrentChannel(defaultChannel);
		this.broadcast(new ServerUsers(this.getConnectedClients().toArray(new UserData[0])));
	}

	private void initializePendingClientConnection(UUID clientId, String pendingUsername, ClientThread clientThread) {
		this.pendingClients.put(clientId, clientThread);
		clientThread.setClientId(clientId);
		clientThread.setClientNickname(pendingUsername);
		clientThread.sendToClient(RegistrationStatus.pending());
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

	public List<UserData> getConnectedClients() {
		return this.clients.values().stream()
				.sorted(Comparator.comparing(ClientThread::getClientNickname))
				.map(ClientThread::toData)
				.collect(Collectors.toList());
	}

	public List<UserData> getPendingClients() {
		return this.pendingClients.values().stream()
				.sorted(Comparator.comparing(ClientThread::getClientNickname))
				.map(ClientThread::toData)
				.collect(Collectors.toList());
	}

	public Set<UUID> getConnectedIds() {
		return this.clients.keySet();
	}

	public Optional<ClientThread> getClientById(UUID id) {
		return Optional.ofNullable(this.clients.get(id));
	}

	public Optional<ClientThread> getPendingClientById(UUID id) {
		return Optional.ofNullable(this.pendingClients.get(id));
	}
}
