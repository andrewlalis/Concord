package nl.andrewl.concord_core.msg.types;

import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This message is sent from the client to a server, to provide identification
 * information about the client to the server when the connection is started.
 */
@Data
@NoArgsConstructor
public class Identification implements Message {
	private String nickname;

	public Identification(String nickname) {
		this.nickname = nickname;
	}

	@Override
	public int getByteCount() {
		return getByteSize(this.nickname);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeString(this.nickname, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.nickname = readString(i);
	}
}
