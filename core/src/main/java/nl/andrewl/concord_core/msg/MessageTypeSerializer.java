package nl.andrewl.concord_core.msg;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Record containing the components needed to read and write a given message.
 * <p>
 *     Also contains methods for automatically generating message type
 *     implementations for standard record-based messages.
 * </p>
 * @param <T> The type of message.
 * @param messageClass The class of the message.
 * @param byteSizeFunction A function that computes the byte size of the message.
 * @param reader A reader that can read messages from an input stream.
 * @param writer A writer that write messages from an input stream.
 */
public record MessageTypeSerializer<T extends Message>(
		Class<T> messageClass,
		Function<T, Integer> byteSizeFunction,
		MessageReader<T> reader,
		MessageWriter<T> writer
) {
	private static final Map<Class<?>, MessageTypeSerializer<?>> generatedMessageTypes = new HashMap<>();

	/**
	 * Gets the {@link MessageTypeSerializer} instance for a given message class, and
	 * generates a new implementation if none exists yet.
	 * @param messageClass The class of the message to get a type for.
	 * @param <T> The type of the message.
	 * @return The message type.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Message> MessageTypeSerializer<T> get(Class<T> messageClass) {
		return (MessageTypeSerializer<T>) generatedMessageTypes.computeIfAbsent(messageClass, c -> generateForRecord((Class<T>) c));
	}

	/**
	 * Generates a message type instance for a given class, using reflection to
	 * introspect the fields of the message.
	 * <p>
	 *     Note that this only works for record-based messages.
	 * </p>
	 * @param messageTypeClass The class of the message type.
	 * @param <T> The type of the message.
	 * @return A message type instance.
	 */
	public static <T extends Message> MessageTypeSerializer<T> generateForRecord(Class<T> messageTypeClass) {
		RecordComponent[] components = messageTypeClass.getRecordComponents();
		Constructor<T> constructor;
		try {
			constructor = messageTypeClass.getDeclaredConstructor(Arrays.stream(components)
					.map(RecordComponent::getType).toArray(Class<?>[]::new));
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		}
		return new MessageTypeSerializer<>(
				messageTypeClass,
				generateByteSizeFunction(components),
				generateReader(constructor),
				generateWriter(components)
		);
	}

	/**
	 * Generates a function implementation that counts the byte size of a
	 * message based on the message's record component types.
	 * @param components The list of components that make up the message.
	 * @param <T> The message type.
	 * @return A function that computes the byte size of a message of the given
	 * type.
	 */
	private static <T extends Message> Function<T, Integer> generateByteSizeFunction(RecordComponent[] components) {
		return msg -> {
			int size = 0;
			for (var component : components) {
				try {
					size += MessageUtils.getByteSize(component.getAccessor().invoke(msg));
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException(e);
				}
			}
			return size;
		};
	}

	/**
	 * Generates a message reader for the given message constructor method. It
	 * will try to read objects from the input stream according to the
	 * parameters of the canonical constructor of a message record.
	 * @param constructor The canonical constructor of the message record.
	 * @param <T> The message type.
	 * @return A message reader for the given type.
	 */
	private static <T extends Message> MessageReader<T> generateReader(Constructor<T> constructor) {
		return in -> {
			Object[] values = new Object[constructor.getParameterCount()];
			for (int i = 0; i < values.length; i++) {
				values[i] = in.readObject(constructor.getParameterTypes()[i]);
			}
			try {
				return constructor.newInstance(values);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException(e);
			}
		};
	}

	/**
	 * Generates a message writer for the given message record components.
	 * @param components The record components to write.
	 * @param <T> The type of message.
	 * @return The message writer for the given type.
	 */
	private static <T extends Message> MessageWriter<T> generateWriter(RecordComponent[] components) {
		return (msg, out) -> {
			for (var component: components) {
				try {
					out.writeObject(component.getAccessor().invoke(msg), component.getType());
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException(e);
				}
			}
		};
	}
}
