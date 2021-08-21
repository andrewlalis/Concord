package nl.andrewl.concord_core.msg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Represents any message which can be sent over the network.
 * <p>
 *     All messages consist of a single byte type identifier, followed by a
 *     payload whose structure depends on the message.
 * </p>
 */
public interface Message {
	/**
	 * @return The exact number of bytes that this message will use when written
	 * to a stream.
	 */
	int getByteCount();

	/**
	 * Writes this message to the given output stream.
	 * @param o The output stream to write to.
	 * @throws IOException If an error occurs while writing.
	 */
	void write(DataOutputStream o) throws IOException;

	/**
	 * Reads all of this message's properties from the given input stream.
	 * <p>
	 *     The single byte type identifier has already been read.
	 * </p>
	 * @param i The input stream to read from.
	 * @throws IOException If an error occurs while reading.
	 */
	void read(DataInputStream i) throws IOException;

	// Utility methods.

	/**
	 * Gets the number of bytes that the given string will occupy when it is
	 * serialized.
	 * @param s The string.
	 * @return The number of bytes used to serialize the string.
	 */
	default int getByteSize(String s) {
		return Integer.BYTES + s.getBytes(StandardCharsets.UTF_8).length;
	}

	/**
	 * Writes a string to the given output stream using a length-prefixed format
	 * where an integer length precedes the string's bytes, which are encoded in
	 * UTF-8.
	 * @param s The string to write.
	 * @param o The output stream to write to.
	 * @throws IOException If the stream could not be written to.
	 */
	default void writeString(String s, DataOutputStream o) throws IOException {
		if (s == null) {
			o.writeInt(-1);
		} else {
			o.writeInt(s.length());
			o.write(s.getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * Reads a string from the given input stream, using a length-prefixed
	 * format, where an integer length precedes the string's bytes, which are
	 * encoded in UTF-8.
	 * @param i The input stream to read from.
	 * @return The string which was read.
	 * @throws IOException If the stream could not be read, or if the string is
	 * malformed.
	 */
	default String readString(DataInputStream i) throws IOException {
		int length = i.readInt();
		if (length == -1) return null;
		byte[] data = new byte[length];
		int read = i.read(data);
		if (read != length) throw new IOException("Not all bytes of a string of length " + length + " could be read.");
		return new String(data, StandardCharsets.UTF_8);
	}

	default void writeEnum(Enum<?> value, DataOutputStream o) throws IOException {
		o.writeInt(value.ordinal());
	}

	default <T extends Enum<?>> T readEnum(Class<T> e, DataInputStream i) throws IOException {
		int ordinal = i.readInt();
		return e.getEnumConstants()[ordinal];
	}
}
