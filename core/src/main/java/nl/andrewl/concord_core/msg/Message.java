package nl.andrewl.concord_core.msg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
}
