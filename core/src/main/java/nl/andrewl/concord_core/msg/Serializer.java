package nl.andrewl.concord_core.msg;

import nl.andrewl.concord_core.msg.types.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is responsible for reading and writing messages from streams.
 */
public class Serializer {
	private static final Map<Byte, Class<? extends Message>> messageTypes = new HashMap<>();
	private static final Map<Class<? extends Message>, Byte> inverseMessageTypes = new HashMap<>();
	static {
		registerType(0, Identification.class);
		registerType(1, ServerWelcome.class);
		registerType(2, Chat.class);
		registerType(3, MoveToChannel.class);
		registerType(4, ChatHistoryRequest.class);
		registerType(5, ChatHistoryResponse.class);
	}

	private static void registerType(int id, Class<? extends Message> clazz) {
		messageTypes.put((byte) id, clazz);
		inverseMessageTypes.put(clazz, (byte) id);
	}

	public static Message readMessage(InputStream i) throws IOException {
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

	public static void writeMessage(Message msg, OutputStream o) throws IOException {
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
