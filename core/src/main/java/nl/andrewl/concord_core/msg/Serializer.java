package nl.andrewl.concord_core.msg;

import nl.andrewl.concord_core.msg.types.Error;
import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_core.msg.types.ServerUsers;
import nl.andrewl.concord_core.msg.types.UserData;
import nl.andrewl.concord_core.msg.types.channel.CreateThread;
import nl.andrewl.concord_core.msg.types.channel.MoveToChannel;
import nl.andrewl.concord_core.msg.types.chat.Chat;
import nl.andrewl.concord_core.msg.types.chat.ChatHistoryRequest;
import nl.andrewl.concord_core.msg.types.chat.ChatHistoryResponse;
import nl.andrewl.concord_core.msg.types.client_setup.*;
import nl.andrewl.concord_core.util.ChainedDataOutputStream;
import nl.andrewl.concord_core.util.ExtendedDataInputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for reading and writing messages from streams. It
 * also defines the set of supported message types, and their associated byte
 * identifiers, via the {@link Serializer#registerType(int, Class)} method.
 */
public class Serializer {
	/**
	 * The mapping which defines each supported message type and the byte value
	 * used to identify it when reading and writing messages.
	 */
	private final Map<Byte, MessageTypeSerializer<?>> messageTypes = new HashMap<>();

	/**
	 * An inverse of {@link Serializer#messageTypes} which is used to look up a
	 * message's byte value when you know the class of the message.
	 */
	private final Map<MessageTypeSerializer<?>, Byte> inverseMessageTypes = new HashMap<>();

	/**
	 * Constructs a new serializer instance, with a standard set of supported
	 * message types.
	 */
	public Serializer() {
		List<Class<? extends Message>> messageClasses = List.of(
				// Utility messages.
				Error.class,
				UserData.class,
				ServerUsers.class,
				// Client setup messages.
				KeyData.class, ClientRegistration.class, ClientLogin.class, ClientSessionResume.class,
				RegistrationStatus.class, ServerWelcome.class, ServerMetaData.class,
				// Chat messages.
				Chat.class, ChatHistoryRequest.class, ChatHistoryResponse.class,
				// Channel messages.
				MoveToChannel.class,
				CreateThread.class
		);
		for (int id = 0; id < messageClasses.size(); id++) {
			registerType(id, messageClasses.get(id));
		}
	}

	/**
	 * Helper method which registers a message type to be supported by the
	 * serializer, by adding it to the normal and inverse mappings.
	 * @param id The byte which will be used to identify messages of the given
	 *           class. The value should from 0 to 127.
	 * @param messageClass The type of message associated with the given id.
	 */
	private synchronized <T extends Message> void registerType(int id, Class<T> messageClass) {
		MessageTypeSerializer<T> type = MessageTypeSerializer.get(messageClass);
		messageTypes.put((byte) id, type);
		inverseMessageTypes.put(type, (byte) id);
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
		ExtendedDataInputStream d = new ExtendedDataInputStream(i);
		byte typeId = d.readByte();
		var type = messageTypes.get(typeId);
		if (type == null) {
			throw new IOException("Unsupported message type: " + typeId);
		}
		try {
			return type.reader().read(d);
		} catch (Throwable e) {
			throw new IOException("Could not instantiate new message object of type " + type.getClass().getSimpleName(), e);
		}
	}

	/**
	 * Writes a message to the given output stream.
	 * @param msg The message to write.
	 * @param o The output stream to write to.
	 * @throws IOException If an error occurs while writing, or if the message
	 * to write is not supported by this serializer.
	 */
	public <T extends Message> void writeMessage(Message msg, OutputStream o) throws IOException {
		DataOutputStream d = new DataOutputStream(o);
		Byte typeId = inverseMessageTypes.get(msg.getTypeSerializer());
		if (typeId == null) {
			throw new IOException("Unsupported message type: " + msg.getClass().getSimpleName());
		}
		d.writeByte(typeId);
		msg.getTypeSerializer().writer().write(msg, new ChainedDataOutputStream(d));
		d.flush();
	}
}
