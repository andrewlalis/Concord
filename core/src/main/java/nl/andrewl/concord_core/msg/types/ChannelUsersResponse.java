package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

/**
 * This message is sent from the server to the client when the information about
 * the users in the channel that a client is in has changed. For example, when
 * a user leaves a channel, all others in that channel will be sent this message
 * to indicate that update.
 * @deprecated Clients will be updated via a {@link ServerUsers} message.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Deprecated
public class ChannelUsersResponse implements Message {
	private List<UserData> users;

	@Override
	public int getByteCount() {
		return getByteSize(this.users);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeList(this.users, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.users = readList(UserData.class, i);
	}
}
