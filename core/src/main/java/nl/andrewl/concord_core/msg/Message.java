package nl.andrewl.concord_core.msg;

/**
 * Represents any message which can be sent over the network.
 * <p>
 *     All messages consist of a single byte type identifier, followed by a
 *     payload whose structure depends on the message.
 * </p>
 */
public interface Message {
	@SuppressWarnings("unchecked")
	default <T extends Message> MessageType<T> getType() {
		return MessageType.get((Class<T>) this.getClass());
	}

	default int byteSize() {
		return getType().byteSizeFunction().apply(this);
	}
}
