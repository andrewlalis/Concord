package nl.andrewl.concord_core.msg.types;

import lombok.Data;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This message is sent from the server to the client after the server accepts
 * the client's identification and registers the client in the server.
 */
@Data
public class ServerWelcome implements Message {
	private long clientId;

	@Override
	public int getByteCount() {
		return Long.BYTES;
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		o.writeLong(this.clientId);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.clientId = i.readLong();
	}
}
