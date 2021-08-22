package nl.andrewl.concord_server;

import lombok.Getter;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single communication area in which messages are sent by clients
 * and received by all connected clients.
 */
@Getter
public class Channel {
	private final ConcordServer server;
	private UUID id;
	private String name;

	private final Set<ClientThread> connectedClients;

	public Channel(ConcordServer server, UUID id, String name) {
		this.server = server;
		this.id = id;
		this.name = name;
		this.connectedClients = ConcurrentHashMap.newKeySet();
	}

	public void addClient(ClientThread clientThread) {
		this.connectedClients.add(clientThread);
	}

	public void removeClient(ClientThread clientThread) {
		this.connectedClients.remove(clientThread);
	}

	/**
	 * Sends a message to all clients that are currently connected to this
	 * channel.
	 * @param msg The message to send.
	 * @throws IOException If an error occurs.
	 */
	public void sendMessage(Message msg) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(msg.getByteCount() + 1);
		Serializer.writeMessage(msg, baos);
		byte[] data = baos.toByteArray();
		for (var client : this.connectedClients) {
			client.sendToClient(data);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Channel channel)) return false;
		return name.equals(channel.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public String toString() {
		return this.name;
	}
}
