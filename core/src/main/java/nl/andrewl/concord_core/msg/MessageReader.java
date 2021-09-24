package nl.andrewl.concord_core.msg;

import nl.andrewl.concord_core.util.ExtendedDataInputStream;

import java.io.IOException;

@FunctionalInterface
public interface MessageReader<T extends Message>{
	/**
	 * Reads all of this message's properties from the given input stream.
	 * <p>
	 *     The single byte type identifier has already been read.
	 * </p>
	 * @param in The input stream to read from.
	 * @return The message that was read.
	 * @throws IOException If an error occurs while reading.
	 */
	T read(ExtendedDataInputStream in) throws IOException;
}
