package nl.andrewl.concord_server;

import lombok.Getter;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.ChannelUsersResponse;
import nl.andrewl.concord_core.msg.types.UserData;
import org.dizitart.no2.IndexOptions;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteCollection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
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

	private final NitriteCollection messageCollection;

	public Channel(ConcordServer server, UUID id, String name, NitriteCollection messageCollection) {
		this.server = server;
		this.id = id;
		this.name = name;
		this.connectedClients = ConcurrentHashMap.newKeySet();
		this.messageCollection = messageCollection;
		this.initCollection();
	}

	/**
	 * Initializes this channel's nitrite database collection, which involves
	 * creating any indexes that don't yet exist.
	 */
	private void initCollection() {
		if (!this.messageCollection.hasIndex("timestamp")) {
			System.out.println("Adding index on \"timestamp\" field to collection " + this.messageCollection.getName());
			this.messageCollection.createIndex("timestamp", IndexOptions.indexOptions(IndexType.NonUnique));
		}
		if (!this.messageCollection.hasIndex("senderNickname")) {
			System.out.println("Adding index on \"senderNickname\" field to collection " + this.messageCollection.getName());
			this.messageCollection.createIndex("senderNickname", IndexOptions.indexOptions(IndexType.Fulltext));
		}
		if (!this.messageCollection.hasIndex("message")) {
			System.out.println("Adding index on \"message\" field to collection " + this.messageCollection.getName());
			this.messageCollection.createIndex("message", IndexOptions.indexOptions(IndexType.Fulltext));
		}
		var fields = List.of("timestamp", "senderNickname", "message");
		for (var index : this.messageCollection.listIndices()) {
			if (!fields.contains(index.getField())) {
				System.out.println("Dropping unknown index " + index.getField() + " from collection " + index.getCollectionName());
				this.messageCollection.dropIndex(index.getField());
			}
		}
	}

	/**
	 * Adds a client to this channel. Also sends an update to all clients,
	 * including the new one, telling them that a user has joined.
	 * @param clientThread The client to add.
	 */
	public void addClient(ClientThread clientThread) {
		this.connectedClients.add(clientThread);
		try {
			this.sendMessage(new ChannelUsersResponse(this.getUserData()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Removes a client from this channel. Also sends an update to all the
	 * clients that are still connected, telling them that a user has left.
	 * @param clientThread The client to remove.
	 */
	public void removeClient(ClientThread clientThread) {
		this.connectedClients.remove(clientThread);
		try {
			this.sendMessage(new ChannelUsersResponse(this.getUserData()));
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream(msg.getByteCount() + 1);
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
		users.sort(Comparator.comparing(UserData::getName));
		return users;
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
		return this.name + " (" + this.id + ")";
	}
}
