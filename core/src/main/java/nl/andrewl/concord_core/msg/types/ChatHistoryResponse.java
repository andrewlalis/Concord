package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

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
		return UUID_BYTES + getByteSize(messages);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeUUID(this.channelId, o);
		writeList(this.messages, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.channelId = readUUID(i);
		System.out.println("Reading list of chats...");
		this.messages = readList(Chat.class, i);
	}
}
