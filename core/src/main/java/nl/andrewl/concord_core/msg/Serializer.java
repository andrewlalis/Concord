package nl.andrewl.concord_core.msg;

import nl.andrewl.concord_core.msg.types.Error;
import nl.andrewl.concord_core.msg.types.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for reading and writing messages from streams.
 */
public class Serializer {
	/**
	 * The mapping which defines each supported message type and the byte value
	 * used to identify it when reading and writing messages.
	 */
	private final Map<Byte, Class<? extends Message>> messageTypes = new HashMap<>();

	/**
	 * An inverse of {@link Serializer#messageTypes} which is used to look up a
	 * message's byte value when you know the class of the message.
	 */
	private final Map<Class<? extends Message>, Byte> inverseMessageTypes = new HashMap<>();

	/**
	 * Constructs a new serializer instance, with a standard set of supported
	 * message types.
	 */
	public Serializer() {
		registerType(0, Identification.class);
		registerType(1, ServerWelcome.class);
		registerType(2, Chat.class);
		registerType(3, MoveToChannel.class);
		registerType(4, ChatHistoryRequest.class);
		registerType(5, ChatHistoryResponse.class);
		registerType(6, ChannelUsersRequest.class);
		registerType(7, ServerUsers.class);
		registerType(8, ServerMetaData.class);
		registerType(9, Error.class);
		registerType(10, CreateThread.class);
	}

	/**
	 * Helper method which registers a message type to be supported by the
	 * serializer, by adding it to the normal and inverse mappings.
	 * @param id The byte which will be used to identify messages of the given
	 *           class. The value should from 0 to 127.
	 * @param messageClass The class of message which is registered with the
	 *                     given byte identifier.
	 */
	private synchronized void registerType(int id, Class<? extends Message> messageClass) {
		messageTypes.put((byte) id, messageClass);
		inverseMessageTypes.put(messageClass, (byte) id);
	}

	/**
	 * Reads a message from the given input stream and returns it, or throws an
	 * exception if an error occurred while reading from the stream.
	 * @param i The input stream to read from.
	 * @return The message which was read.
	 * @throws IOException If an error occurs while reading, such as trying to
	 * read an unsupported message type, or if a message object could not be
	 * constructed for the incoming data.
	 */
	public Message readMessage(InputStream i) throws IOException {
		DataInputStream d = new DataInputStream(i);
		byte type = d.readByte();
		var clazz = messageTypes.get(type);
		if (clazz == null) {
			throw new IOException("Unsupported message type: " + type);
		}
		try {
			var constructor = clazz.getConstructor();
			var message = constructor.newInstance();
			message.read(d);
			return message;
		} catch (Throwable e) {
			throw new IOException("Could not instantiate new message object of type " + clazz.getSimpleName(), e);
		}
	}

	/**
	 * Writes a message to the given output stream.
	 * @param msg The message to write.
	 * @param o The output stream to write to.
	 * @throws IOException If an error occurs while writing, or if the message
	 * to write is not supported by this serializer.
	 */
	public void writeMessage(Message msg, OutputStream o) throws IOException {
		DataOutputStream d = new DataOutputStream(o);
		Byte type = inverseMessageTypes.get(msg.getClass());
		if (type == null) {
			throw new IOException("Unsupported message type: " + msg.getClass().getSimpleName());
		}
		d.writeByte(type);
		msg.write(d);
		d.flush();
	}
}
