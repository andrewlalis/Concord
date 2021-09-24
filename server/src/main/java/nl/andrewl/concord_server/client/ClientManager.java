package nl.andrewl.concord_server.client;

import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.Error;
import nl.andrewl.concord_core.msg.types.ServerUsers;
import nl.andrewl.concord_core.msg.types.UserData;
import nl.andrewl.concord_core.msg.types.client_setup.Identification;
import nl.andrewl.concord_core.msg.types.client_setup.ServerWelcome;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.util.CollectionUtils;
import nl.andrewl.concord_server.util.StringUtils;
import org.dizitart.no2.Document;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.filters.Filters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The client manager is responsible for managing the set of clients connected
 * to a server.
 */
public class ClientManager {
	private final ConcordServer server;
	private final Map<UUID, ClientThread> clients;
	private final NitriteCollection userCollection;

	public ClientManager(ConcordServer server) {
		this.server = server;
		this.clients = new ConcurrentHashMap<>();
		this.userCollection = server.getDb().getCollection("users");
		CollectionUtils.ensureIndexes(this.userCollection, Map.of(
				"id", IndexType.Unique,
				"sessionToken", IndexType.Unique,
				"nickname", IndexType.Fulltext
		));
	}

	/**
	 * Registers a new client as connected to the server. This is done once the
	 * client thread has received the correct identification information from
	 * the client. The server will register the client in its global set of
	 * connected clients, and it will immediately move the client to the default
	 * channel.
	 * <p>
	 *     If the client provides a session token with their identification
	 *     message, then we should load their data from our database, otherwise
	 *     we assume this is a new client.
	 * </p>
	 * @param identification The client's identification data.
	 * @param clientThread The client manager thread.
	 */
	public void handleLogIn(Identification identification, ClientThread clientThread) {
		ClientConnectionData data;
		try {
			data = identification.sessionToken() == null ? getNewClientData(identification) : getClientDataFromDb(identification);
		} catch (InvalidIdentificationException e) {
			clientThread.sendToClient(Error.warning(e.getMessage()));
			return;
		}

		this.clients.put(data.id, clientThread);
		clientThread.setClientId(data.id);
		clientThread.setClientNickname(data.nickname);
		var defaultChannel = this.server.getChannelManager().getDefaultChannel().orElseThrow();
		clientThread.sendToClient(new ServerWelcome(data.id, data.sessionToken, defaultChannel.getId(), defaultChannel.getName(), this.server.getMetaData()));
		// It is important that we send the welcome message first. The client expects this as the initial response to their identification message.
		defaultChannel.addClient(clientThread);
		clientThread.setCurrentChannel(defaultChannel);
		System.out.printf(
				"Client %s(%s) joined%s, and was put into %s.\n",
				data.nickname,
				data.id,
				data.newClient ? " for the first time" : "",
				defaultChannel
		);
		this.broadcast(new ServerUsers(this.getClients().toArray(new UserData[0])));
	}

	/**
	 * De-registers a client from the server, removing them from any channel
	 * they're currently in.
	 * @param clientId The id of the client to remove.
	 */
	public void handleLogOut(UUID clientId) {
		var client = this.clients.remove(clientId);
		if (client != null) {
			client.getCurrentChannel().removeClient(client);
			client.shutdown();
			System.out.println("Client " + client + " has disconnected.");
			this.broadcast(new ServerUsers(this.getClients().toArray(new UserData[0])));
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

	public List<UserData> getClients() {
		return this.clients.values().stream()
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

	private static record ClientConnectionData(UUID id, String nickname, String sessionToken, boolean newClient) {}

	private ClientConnectionData getClientDataFromDb(Identification identification) throws InvalidIdentificationException {
		var cursor = this.userCollection.find(Filters.eq("sessionToken", identification.sessionToken()));
		Document doc = cursor.firstOrDefault();
		if (doc != null) {
			UUID id = doc.get("id", UUID.class);
			String nickname = identification.nickname();
			if (nickname != null) {
				doc.put("nickname", nickname);
			} else {
				nickname = doc.get("nickname", String.class);
			}
			String sessionToken = StringUtils.random(128);
			doc.put("sessionToken", sessionToken);
			this.userCollection.update(doc);
			return new ClientConnectionData(id, nickname, sessionToken, false);
		} else {
			throw new InvalidIdentificationException("Invalid session token.");
		}
	}

	private ClientConnectionData getNewClientData(Identification identification) throws InvalidIdentificationException {
		UUID id = this.server.getIdProvider().newId();
		String nickname = identification.nickname();
		if (nickname == null) {
			throw new InvalidIdentificationException("Missing nickname.");
		}
		String sessionToken = StringUtils.random(128);
		Document doc = new Document(Map.of(
				"id", id,
				"nickname", nickname,
				"sessionToken", sessionToken,
				"createdAt", System.currentTimeMillis()
		));
		this.userCollection.insert(doc);
		return new ClientConnectionData(id, nickname, sessionToken, true);
	}
}
