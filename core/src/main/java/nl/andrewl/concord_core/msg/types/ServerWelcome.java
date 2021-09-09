package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

/**
 * This message is sent from the server to the client after the server accepts
 * the client's identification and registers the client in the server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerWelcome implements Message {
	/**
	 * The unique id of this client.
	 */
	private UUID clientId;

	/**
	 * The token which this client can use to reconnect to the server later and
	 * still be recognized as the same user.
	 */
	private String sessionToken;

	/**
	 * The id of the channel that the user has been placed in.
	 */
	private UUID currentChannelId;

	/**
	 * The name of the channel that the user has been placed in.
	 */
	private String currentChannelName;

	/**
	 * Information about the server's structure.
	 */
	private ServerMetaData metaData;

	@Override
	public int getByteCount() {
		return 2 * UUID_BYTES + getByteSize(this.sessionToken) + getByteSize(this.currentChannelName) + this.metaData.getByteCount();
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeUUID(this.clientId, o);
		writeString(this.sessionToken, o);
		writeUUID(this.currentChannelId, o);
		writeString(this.currentChannelName, o);
		this.metaData.write(o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.clientId = readUUID(i);
		this.sessionToken = readString(i);
		this.currentChannelId = readUUID(i);
		this.metaData = new ServerMetaData();
		this.currentChannelName = readString(i);
		this.metaData.read(i);
	}
}
