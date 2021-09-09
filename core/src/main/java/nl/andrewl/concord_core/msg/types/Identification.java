package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

/**
 * This message is sent from the client to a server, to provide identification
 * information about the client to the server when the connection is started.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Identification implements Message {
	/**
	 * The nickname that a client wants to be identified by when in the server.
	 * If a valid session token is provided, this can be left as null, and the
	 * user will be given the same nickname they had in their previous session.
	 */
	private String nickname;

	/**
	 * A session token that's used to uniquely identify this client as the same
	 * as one who has previously connected to the server. If this is null, the
	 * client is indicating that they have not connected to this server before.
	 */
	private String sessionToken;

	public Identification(String nickname) {
		this.nickname = nickname;
	}

	@Override
	public int getByteCount() {
		return getByteSize(this.nickname) + getByteSize(sessionToken);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeString(this.nickname, o);
		writeString(this.sessionToken, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.nickname = readString(i);
		this.sessionToken = readString(i);
	}
}
