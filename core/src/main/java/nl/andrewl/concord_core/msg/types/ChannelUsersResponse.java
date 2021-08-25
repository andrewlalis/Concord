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

@Data
@NoArgsConstructor
@AllArgsConstructor
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
		try {
			this.users = readList(UserData.class, i);
		} catch (ReflectiveOperationException e) {
			throw new IOException(e);
		}
	}
}
