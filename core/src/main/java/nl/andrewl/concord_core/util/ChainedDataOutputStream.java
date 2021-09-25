package nl.andrewl.concord_core.util;

import nl.andrewl.concord_core.msg.Message;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * A more complex output stream which redefines certain methods for convenience
 * with method chaining.
 */
public class ChainedDataOutputStream {
	private final DataOutputStream out;

	public ChainedDataOutputStream(DataOutputStream out) {
		this.out = out;
	}

	public ChainedDataOutputStream writeInt(int x) throws IOException {
		out.writeInt(x);
		return this;
	}

	public ChainedDataOutputStream writeString(String s) throws IOException {
		if (s == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(s.length());
			out.write(s.getBytes(StandardCharsets.UTF_8));
		}
		return this;
	}

	public ChainedDataOutputStream writeStrings(String... strings) throws IOException {
		for (var s : strings) {
			writeString(s);
		}
		return this;
	}

	public ChainedDataOutputStream writeEnum(Enum<?> value) throws IOException {
		if (value == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(value.ordinal());
		}
		return this;
	}

	public ChainedDataOutputStream writeUUID(UUID uuid) throws IOException {
		if (uuid == null) {
			out.writeLong(-1);
			out.writeLong(-1);
		} else {
			out.writeLong(uuid.getMostSignificantBits());
			out.writeLong(uuid.getLeastSignificantBits());
		}
		return this;
	}

	public <T extends Message> ChainedDataOutputStream writeArray(T[] array) throws IOException {
		this.out.writeInt(array.length);
		for (var item : array) {
			writeMessage(item);
		}
		return this;
	}

	public <T extends Message> ChainedDataOutputStream writeMessage(Message msg) throws IOException {
		this.out.writeBoolean(msg != null);
		if (msg != null) {
			msg.getTypeSerializer().writer().write(msg, this);
		}
		return this;
	}

	/**
	 * Writes an object to the stream.
	 * @param o The object to write.
	 * @param type The object's type. This is needed in case the object itself
	 *             is null, which may be the case for some strings or ids.
	 * @return The chained output stream.
	 * @throws IOException If an error occurs.
	 */
	public ChainedDataOutputStream writeObject(Object o, Class<?> type) throws IOException {
		if (type.equals(Integer.class) || type.equals(int.class)) {
			this.writeInt((Integer) o);
		} else if (type.equals(Long.class) || type.equals(long.class)) {
			this.out.writeLong((Long) o);
		} else if (type.equals(String.class)) {
			this.writeString((String) o);
		} else if (type.equals(UUID.class)) {
			this.writeUUID((UUID) o);
		} else if (type.isEnum()) {
			this.writeEnum((Enum<?>) o);
		} else if (type.equals(byte[].class)) {
			byte[] b = (byte[]) o;
			this.writeInt(b.length);
			this.out.write(b);
		} else if (type.isArray() && Message.class.isAssignableFrom(type.getComponentType())) {
			this.writeArray((Message[]) o);
		} else if (Message.class.isAssignableFrom(type)) {
			this.writeMessage((Message) o);
		} else {
			throw new IOException("Unsupported object type: " + o.getClass().getSimpleName());
		}
		return this;
	}
}
