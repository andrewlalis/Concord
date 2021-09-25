package nl.andrewl.concord_core.msg;

/**
 * Represents any message which can be sent over the network.
 * <p>
 *     All messages consist of a single byte type identifier, followed by a
 *     payload whose structure depends on the message.
 * </p>
 */
public interface Message {
	/**
	 * Convenience method to get the serializer for this message's type, using
	 * the static auto-generated set of serializers.
	 * @param <T> The message type.
	 * @return The serializer to use to read and write messages of this type.
	 */
	@SuppressWarnings("unchecked")
	default <T extends Message> MessageTypeSerializer<T> getTypeSerializer() {
		return MessageTypeSerializer.get((Class<T>) this.getClass());
	}

	/**
	 * Convenience method to determine the size of this message in bytes.
	 * @return The size of this message, in bytes.
	 */
	default int byteSize() {
		return getTypeSerializer().byteSizeFunction().apply(this);
	}
}
