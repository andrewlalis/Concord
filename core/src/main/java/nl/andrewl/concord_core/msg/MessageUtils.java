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

	public static final char MIN_HIGH_SURROGATE = '\uD800';
	public static final char MAX_HIGH_SURROGATE = '\uDBFF';
	public static final char MIN_LOW_SURROGATE = '\uDC00';
	public static final char MAX_LOW_SURROGATE = '\uDFFF';
	public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
	private static final int SUR_CALC = (MIN_SUPPLEMENTARY_CODE_POINT - (MIN_HIGH_SURROGATE << 10)) - MIN_LOW_SURROGATE;


	/**
	 * Gets the number of bytes that the given string will occupy when it is
	 * serialized.
	 * @param s The string. This may be null.
	 * @return The number of bytes used to serialize the string.
	 */
	public static int getByteSize(String s) {
		return Integer.BYTES + (s == null ? 0 : s.getBytes(StandardCharsets.UTF_8).length);
//		int length = s.length();
//		int i = 0;
//		int counter = 0;
//
//		char c;
//		while (i < length && (c = s.charAt(i)) < '\u0080') {
//			// ascii fast loop;
//			counter++;
//			i++;
//		}
//
//		while (i < length) {
//			c = s.charAt(i++);
//			if (c < 0x80) {
//				counter++;
//			} else if (c < 0x800) {
//				counter += 2;
//			} else if (Character.isSurrogate(c)) {
//				int uc = -1;
//				char c2;
//				if (isHighSurrogate(c) && i < length && isLowSurrogate(c2 = s.charAt(i))) {
//					uc = toCodePoint(c, c2);
//				}
//				if (uc < 0) {
//
//				} else {
//					counter += 4;
//					i++;  // 2 chars
//				}
//			} else {
//				// 3 bytes, 16 bits
//				counter += 3;
//			}
//		}
//
//		return Integer.BYTES + counter;
	}
	public static boolean isHighSurrogate(char ch) {
		return ch >= MIN_HIGH_SURROGATE && ch < (MAX_HIGH_SURROGATE + 1);
	}

	public static boolean isLowSurrogate(char ch) {
		return ch >= MIN_LOW_SURROGATE && ch < (MAX_LOW_SURROGATE + 1);
	}

	public static int toCodePoint(int high, int low) {
		return ((high << 10) + low) + SUR_CALC;
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
		if (length == 0) return "";
		byte[] data = new byte[length];
		int read = i.read(data);
		if (read != length) throw new IOException("Not all bytes of a string of length " + length + " could be read.");
		return new String(data, StandardCharsets.UTF_8);
	}

	public static void writeEnum(Enum<?> value, DataOutputStream o) throws IOException {
		if (value == null) {
			o.writeInt(-1);
		} else {
			o.writeInt(value.ordinal());
		}
	}

	public static <T extends Enum<?>> T readEnum(Class<T> e, DataInputStream i) throws IOException {
		int ordinal = i.readInt();
		if (ordinal == -1) return null;
		return e.getEnumConstants()[ordinal];
	}

	public static void writeUUID(UUID value, DataOutputStream o) throws IOException {
		if (value == null) {
			o.writeLong(-1);
			o.writeLong(-1);
		} else {
			o.writeLong(value.getMostSignificantBits());
			o.writeLong(value.getLeastSignificantBits());
		}
	}

	public static UUID readUUID(DataInputStream i) throws IOException {
		long a = i.readLong();
		long b = i.readLong();
		if (a == -1 && b == -1) {
			return null;
		}
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
			System.out.println("Wrote " + i);
		}
	}

	public static <T extends Message> List<T> readList(Class<T> type, DataInputStream i) throws IOException {
		int size = i.readInt();
		System.out.println("Read a size of " + size + " items of type " + type.getSimpleName());
		try {
			var constructor = type.getConstructor();
			List<T> items = new ArrayList<>(size);
			for (int k = 0; k < size; k++) {
				var item = constructor.newInstance();
				item.read(i);
				items.add(item);
				System.out.println("Read item " + (k+1) + " of " + size + ": " + item);
			}
			return items;
		} catch (ReflectiveOperationException e) {
			throw new IOException(e);
		}
	}
}
