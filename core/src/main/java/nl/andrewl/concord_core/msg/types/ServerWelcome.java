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
	private UUID clientId;
	private UUID currentChannelId;
	private String currentChannelName;
	private ServerMetaData metaData;

	@Override
	public int getByteCount() {
		return 2 * UUID_BYTES + getByteSize(this.currentChannelName) + this.metaData.getByteCount();
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeUUID(this.clientId, o);
		writeUUID(this.currentChannelId, o);
		writeString(this.currentChannelName, o);
		this.metaData.write(o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.clientId = readUUID(i);
		this.currentChannelId = readUUID(i);
		this.metaData = new ServerMetaData();
		this.currentChannelName = readString(i);
		this.metaData.read(i);
	}
}
