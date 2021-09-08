package nl.andrewl.concord_core.msg.types;

import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

/**
 * This message is sent by clients when they indicate that they would like to
 * create a new thread in their current channel.
 * <p>
 *     Conversely, this message is also sent by the server when a thread has
 *     been created by someone, and all clients need to be notified so that they
 *     can properly display to the user that a message has been turned into a
 *     thread.
 * </p>
 */
@Data
@NoArgsConstructor
public class CreateThread implements Message {
	/**
	 * The id of the message from which the thread will be created. This will
	 * serve as the entry point of the thread, and the unique identifier for the
	 * thread.
	 */
	private UUID messageId;

	/**
	 * The title for the thread. This may be null, in which case the thread does
	 * not have any title.
	 */
	private String title;

	@Override
	public int getByteCount() {
		return UUID_BYTES + getByteSize(title);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeUUID(this.messageId, o);
		writeString(this.title, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.messageId = readUUID(i);
		this.title = readString(i);
	}
}
