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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelUsersRequest implements Message {
	private UUID channelId;

	@Override
	public int getByteCount() {
		return UUID_BYTES;
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeUUID(this.channelId, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.channelId = readUUID(i);
	}
}
