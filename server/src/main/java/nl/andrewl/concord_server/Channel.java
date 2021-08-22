package nl.andrewl.concord_server;

import lombok.Getter;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.ChannelUsersResponse;
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
	}

	public void addClient(ClientThread clientThread) {
		this.connectedClients.add(clientThread);
		try {
			this.sendMessage(new ChannelUsersResponse(this.getUserData()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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

	public List<ChannelUsersResponse.UserData> getUserData() {
		List<ChannelUsersResponse.UserData> users = new ArrayList<>();
		for (var clientThread : this.getConnectedClients()) {
			users.add(new ChannelUsersResponse.UserData(clientThread.getClientId(), clientThread.getClientNickname()));
		}
		users.sort(Comparator.comparing(ChannelUsersResponse.UserData::getName));
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
		return this.name;
	}
}
