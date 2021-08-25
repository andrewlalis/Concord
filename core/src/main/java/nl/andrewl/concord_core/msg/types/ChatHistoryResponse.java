package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.MessageUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * The response that a server sends to a {@link ChatHistoryRequest}. The list of
 * messages is ordered by timestamp, with the newest messages appearing first.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse implements Message {
	private UUID channelId;
	List<Chat> messages;

	@Override
	public int getByteCount() {
		int count = Long.BYTES + Integer.BYTES;
		for (var message : this.messages) {
			count += message.getByteCount();
		}
		return count;
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		MessageUtils.writeUUID(this.channelId, o);
		o.writeInt(messages.size());
		for (var message : this.messages) {
			message.write(o);
		}
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.channelId = MessageUtils.readUUID(i);
		int messageCount = i.readInt();
		Chat[] messages = new Chat[messageCount];
		for (int k = 0; k < messageCount; k++) {
			Chat c = new Chat();
			c.read(i);
			messages[k] = c;
		}
		this.messages = List.of(messages);
	}
}
