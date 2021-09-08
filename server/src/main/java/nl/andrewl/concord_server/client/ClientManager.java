package nl.andrewl.concord_server.client;

import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.Identification;
import nl.andrewl.concord_core.msg.types.ServerUsers;
import nl.andrewl.concord_core.msg.types.ServerWelcome;
import nl.andrewl.concord_core.msg.types.UserData;
import nl.andrewl.concord_server.ConcordServer;

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

	public ClientManager(ConcordServer server) {
		this.server = server;
		this.clients = new ConcurrentHashMap<>();
	}

	/**
	 * Registers a new client as connected to the server. This is done once the
	 * client thread has received the correct identification information from
	 * the client. The server will register the client in its global set of
	 * connected clients, and it will immediately move the client to the default
	 * channel.
	 * @param identification The client's identification data.
	 * @param clientThread The client manager thread.
	 */
	public void registerClient(Identification identification, ClientThread clientThread) {
		var id = this.server.getIdProvider().newId();
		System.out.printf("Client \"%s\" joined with id %s.\n", identification.getNickname(), id);
		this.clients.put(id, clientThread);
		clientThread.setClientId(id);
		clientThread.setClientNickname(identification.getNickname());
		// Immediately add the client to the default channel and send the initial welcome message.
		var defaultChannel = this.server.getChannelManager().getDefaultChannel().orElseThrow();
		clientThread.sendToClient(new ServerWelcome(id, defaultChannel.getId(), defaultChannel.getName(), this.server.getMetaData()));
		// It is important that we send the welcome message first. The client expects this as the initial response to their identification message.
		defaultChannel.addClient(clientThread);
		clientThread.setCurrentChannel(defaultChannel);
		System.out.println("Moved client " + clientThread + " to " + defaultChannel);
		this.broadcast(new ServerUsers(this.getClients()));
	}

	/**
	 * De-registers a client from the server, removing them from any channel
	 * they're currently in.
	 * @param clientId The id of the client to remove.
	 */
	public void deregisterClient(UUID clientId) {
		var client = this.clients.remove(clientId);
		if (client != null) {
			client.getCurrentChannel().removeClient(client);
			client.shutdown();
			System.out.println("Client " + client + " has disconnected.");
			this.broadcast(new ServerUsers(this.getClients()));
		}
	}

	/**
	 * Sends a message to every connected client, ignoring any channels. All
	 * clients connected to this server will receive this message.
	 * @param message The message to send.
	 */
	public void broadcast(Message message) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(message.getByteCount());
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
}
