package nl.andrewl.concord_core.msg.types;

import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

/**
 * This message contains information about a chat message that a user sent.
 */
@Data
@NoArgsConstructor
public class Chat implements Message {
	private UUID senderId;
	private String senderNickname;
	private long timestamp;
	private String message;

	public Chat(UUID senderId, String senderNickname, long timestamp, String message) {
		this.senderId = senderId;
		this.senderNickname = senderNickname;
		this.timestamp = timestamp;
		this.message = message;
	}

	public Chat(String message) {
		this(null, null, System.currentTimeMillis(), message);
	}

	@Override
	public int getByteCount() {
		return UUID_BYTES + Long.BYTES + getByteSize(this.senderNickname) + getByteSize(this.message);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeUUID(this.senderId, o);
		writeString(this.senderNickname, o);
		o.writeLong(this.timestamp);
		writeString(this.message, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.senderId = readUUID(i);
		this.senderNickname = readString(i);
		this.timestamp = i.readLong();
		this.message = readString(i);
	}

	@Override
	public String toString() {
		return String.format("%s: %s", this.senderNickname, this.message);
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			Chat other = (Chat) o;
			return this.getSenderId().equals(other.getSenderId()) &&
					this.getTimestamp() == other.getTimestamp() &&
					this.getSenderNickname().equals(other.getSenderNickname()) &&
					this.message.length() == other.message.length();
		}
		return false;
	}
}
