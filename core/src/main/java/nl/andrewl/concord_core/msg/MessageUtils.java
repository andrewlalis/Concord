package nl.andrewl.concord_core.msg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class which provides method for serializing and deserializing complex
 * data types.
 */
public class MessageUtils {
	public static final int UUID_BYTES = 2 * Long.BYTES;

	/**
	 * Gets the number of bytes that the given string will occupy when it is
	 * serialized.
	 * @param s The string.
	 * @return The number of bytes used to serialize the string.
	 */
	public static int getByteSize(String s) {
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
	public static void writeString(String s, DataOutputStream o) throws IOException {
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
	public static String readString(DataInputStream i) throws IOException {
		int length = i.readInt();
		if (length == -1) return null;
		byte[] data = new byte[length];
		int read = i.read(data);
		if (read != length) throw new IOException("Not all bytes of a string of length " + length + " could be read.");
		return new String(data, StandardCharsets.UTF_8);
	}

	public static void writeEnum(Enum<?> value, DataOutputStream o) throws IOException {
		o.writeInt(value.ordinal());
	}

	public static <T extends Enum<?>> T readEnum(Class<T> e, DataInputStream i) throws IOException {
		int ordinal = i.readInt();
		return e.getEnumConstants()[ordinal];
	}

	public static void writeUUID(UUID value, DataOutputStream o) throws IOException {
		o.writeLong(value.getMostSignificantBits());
		o.writeLong(value.getLeastSignificantBits());
	}

	public static UUID readUUID(DataInputStream i) throws IOException {
		long a = i.readLong();
		long b = i.readLong();
		return new UUID(a, b);
	}

	public static int getByteSize(List<? extends Message> items) {
		int count = Integer.BYTES;
		for (var item : items) {
			count += item.getByteCount();
		}
		return count;
	}

	public static void writeList(List<? extends Message> items, DataOutputStream o) throws IOException {
		o.writeInt(items.size());
		for (var i : items) {
			i.write(o);
		}
	}

	public static <T extends Message> List<T> readList(Class<T> type, DataInputStream i) throws IOException, ReflectiveOperationException {
		int size = i.readInt();
		var constructor = type.getConstructor();
		List<T> items = new ArrayList<>(size);
		for (int k = 0; k < size; k++) {
			var item = constructor.newInstance();
			item.read(i);
			items.add(item);
		}
		return items;
	}
}
