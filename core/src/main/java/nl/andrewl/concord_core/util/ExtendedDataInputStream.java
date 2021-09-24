package nl.andrewl.concord_core.util;

import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.MessageType;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * An extended output stream which contains additional methods for reading more
 * complex types that are used by the Concord system.
 */
public class ExtendedDataInputStream extends DataInputStream {
	public ExtendedDataInputStream(InputStream in) {
		super(in);
	}

	public String readString() throws IOException {
		int length = super.readInt();
		if (length == -1) return null;
		if (length == 0) return "";
		byte[] data = new byte[length];
		int read = super.read(data);
		if (read != length) throw new IOException("Not all bytes of a string of length " + length + " could be read.");
		return new String(data, StandardCharsets.UTF_8);
	}

	public <T extends Enum<?>> T readEnum(Class<T> e) throws IOException {
		int ordinal = super.readInt();
		if (ordinal == -1) return null;
		return e.getEnumConstants()[ordinal];
	}

	public UUID readUUID() throws IOException {
		long a = super.readLong();
		long b = super.readLong();
		if (a == -1 && b == -1) {
			return null;
		}
		return new UUID(a, b);
	}

	@SuppressWarnings("unchecked")
	public <T extends Message> T[] readArray(MessageType<T> type) throws IOException {
		int length = super.readInt();
		T[] array = (T[]) Array.newInstance(type.messageClass(), length);
		for (int i = 0; i < length; i++) {
			array[i] = type.reader().read(this);
		}
		return array;
	}

	/**
	 * Reads an object from the stream that is of a certain expected type.
	 * @param type The type of object to read.
	 * @return The object that was read.
	 * @throws IOException If an error occurs while reading.
	 */
	@SuppressWarnings("unchecked")
	public Object readObject(Class<?> type) throws IOException {
		if (type.equals(Integer.class) || type.equals(int.class)) {
			return this.readInt();
		} else if (type.equals(Long.class) || type.equals(long.class)) {
			return this.readLong();
		} else if (type.equals(String.class)) {
			return this.readString();
		} else if (type.equals(UUID.class)) {
			return this.readUUID();
		} else if (type.isEnum()) {
			return this.readEnum((Class<? extends Enum<?>>) type);
		} else if (type.isAssignableFrom(byte[].class)) {
			int length = this.readInt();
			return this.readNBytes(length);
		} else if (type.isArray() && Message.class.isAssignableFrom(type.getComponentType())) {
			var messageType = MessageType.get((Class<? extends Message>) type.getComponentType());
			return this.readArray(messageType);
		} else if (Message.class.isAssignableFrom(type)) {
			var messageType = MessageType.get((Class<? extends Message>) type);
			return messageType.reader().read(this);
		} else {
			throw new IOException("Unsupported object type: " + type.getSimpleName());
		}
	}
}
