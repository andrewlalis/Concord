package nl.andrewl.concord_core.msg.types.chat;

import nl.andrewl.concord_core.msg.Message;

import java.util.Objects;
import java.util.UUID;

/**
 * This message contains information about a chat message that a user sent.
 */
public record Chat (
		UUID id, UUID senderId, String senderNickname, long timestamp, String message
) implements Message {
	public Chat(UUID senderId, String senderNickname, long timestamp, String message) {
		this(null, senderId, senderNickname, timestamp, message);
	}

	public Chat(String message) {
		this(null, null, System.currentTimeMillis(), message);
	}

	public Chat(UUID newId, Chat original) {
		this(newId, original.senderId, original.senderNickname, original.timestamp, original.message);
	}

	@Override
	public String toString() {
		return String.format("%s: %s", this.senderNickname, this.message);
	}

	@Override
	public boolean equals(Object o) {
		if (o.getClass().equals(this.getClass())) {
			Chat other = (Chat) o;
			if (Objects.equals(this.id, other.id)) return true;
			return this.senderId.equals(other.senderId) &&
					this.timestamp == other.timestamp &&
					this.senderNickname.equals(other.senderNickname) &&
					this.message.length() == other.message.length();
		}
		return false;
	}
}
