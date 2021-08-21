package nl.andrewl.concord_core.msg.types;

import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * The response that a server sends to a {@link ChatHistoryRequest}.
 */
@Data
@NoArgsConstructor
public class ChatHistoryResponse implements Message {
	private long sourceId;
	private ChatHistoryRequest.Source sourceType;
	List<Chat> messages;

	@Override
	public int getByteCount() {
		int count = Long.BYTES + Integer.BYTES + Integer.BYTES;
		for (var message : this.messages) {
			count += message.getByteCount();
		}
		return count;
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		o.writeLong(this.sourceId);
		writeEnum(this.sourceType, o);
		o.writeInt(messages.size());
		for (var message : this.messages) {
			message.write(o);
		}
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.sourceId = i.readInt();
		this.sourceType = readEnum(ChatHistoryRequest.Source.class, i);
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
