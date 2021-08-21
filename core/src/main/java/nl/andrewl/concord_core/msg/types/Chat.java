package nl.andrewl.concord_core.msg.types;

import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This message contains information about a chat message that a user sent.
 */
@Data
@NoArgsConstructor
public class Chat implements Message {
	private long senderId;
	private String senderNickname;
	private long timestamp;
	private String message;

	public Chat(long senderId, String senderNickname, long timestamp, String message) {
		this.senderId = senderId;
		this.senderNickname = senderNickname;
		this.timestamp = timestamp;
		this.message = message;
	}

	public Chat(String message) {
		this(-1, null, System.currentTimeMillis(), message);
	}

	@Override
	public int getByteCount() {
		return 2 * Long.BYTES + getByteSize(this.message) + getByteSize(this.senderNickname);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		o.writeLong(this.senderId);
		writeString(this.senderNickname, o);
		o.writeLong(this.timestamp);
		writeString(this.message, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.senderId = i.readLong();
		this.senderNickname = readString(i);
		this.timestamp = i.readLong();
		this.message = readString(i);
	}

	@Override
	public String toString() {
		return String.format("%s(%d): %s", this.senderNickname, this.senderId, this.message);
	}
}
