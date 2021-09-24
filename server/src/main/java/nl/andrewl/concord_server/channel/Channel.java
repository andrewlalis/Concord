package nl.andrewl.concord_server.channel;

import lombok.Getter;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.UserData;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.client.ClientThread;
import nl.andrewl.concord_server.util.CollectionUtils;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteCollection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single communication area in which messages are sent by clients
 * and received by all connected clients. A channel is a top-level communication
 * medium, and usually this is a server channel or private message between two
 * clients in a server.
 */
@Getter
public class Channel implements Comparable<Channel> {
	private final ConcordServer server;
	private final UUID id;
	private String name;

	/**
	 * The set of clients that are connected to this channel.
	 */
	private final Set<ClientThread> connectedClients;

	/**
	 * A document collection which holds all messages created in this channel,
	 * indexed on id, timestamp, message, and sender's nickname.
	 */
	private final NitriteCollection messageCollection;

	public Channel(ConcordServer server, UUID id, String name) {
		this.server = server;
		this.id = id;
		this.name = name;
		this.connectedClients = ConcurrentHashMap.newKeySet();
		this.messageCollection = server.getDb().getCollection("channel-" + id);
		CollectionUtils.ensureIndexes(this.messageCollection, Map.of(
				"timestamp", IndexType.NonUnique,
				"senderNickname", IndexType.Fulltext,
				"message", IndexType.Fulltext,
				"id", IndexType.Unique
		));
	}

	/**
	 * Adds a client to this channel.
	 * @param clientThread The client to add.
	 */
	public void addClient(ClientThread clientThread) {
		this.connectedClients.add(clientThread);
	}

	/**
	 * Removes a client from this channel.
	 * @param clientThread The client to remove.
	 */
	public void removeClient(ClientThread clientThread) {
		this.connectedClients.remove(clientThread);
	}

	/**
	 * Sends a message to all clients that are currently connected to this
	 * channel. Makes use of the server's serializer to preemptively serialize
	 * the data once, so that clients need only write a byte array to their
	 * respective output streams.
	 * @param msg The message to send.
	 * @throws IOException If an error occurs.
	 */
	public void sendMessage(Message msg) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(msg.byteSize() + 1);
		this.server.getSerializer().writeMessage(msg, baos);
		byte[] data = baos.toByteArray();
		for (var client : this.connectedClients) {
			client.sendToClient(data);
		}
	}

	/**
	 * Gets a list of information about each user in this channel.
	 * @return A list of {@link UserData} objects.
	 */
	public List<UserData> getUserData() {
		List<UserData> users = new ArrayList<>(this.connectedClients.size());
		for (var clientThread : this.getConnectedClients()) {
			users.add(clientThread.toData());
		}
		users.sort(Comparator.comparing(UserData::name));
		return users;
	}

	public String getAsTag() {
		return "#" + this.name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Channel channel)) return false;
		if (Objects.equals(this.id, channel.getId())) return true;
		return Objects.equals(this.name, channel.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name);
	}

	@Override
	public String toString() {
		return this.name + " (" + this.id + ")";
	}

	@Override
	public int compareTo(Channel o) {
		return this.getName().compareTo(o.getName());
	}
}
