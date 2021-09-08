package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

/**
 * A message that's sent to a client when they've been moved to another channel.
 * This indicates to the client that they should perform the necessary requests
 * to update their view to indicate that they're now in a different channel.
 * <p>
 *     Conversely, a client can send this request to the server to indicate that
 *     they would like to switch to the specified channel.
 * </p>
 * <p>
 *     Clients can also send this message and provide the id of another client
 *     to request that they enter a private message channel with the referenced
 *     client.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MoveToChannel implements Message {
	/**
	 * The id of the channel that the client is requesting or being moved to, or
	 * the id of another client that the user wishes to begin private messaging
	 * with.
	 */
	private UUID id;

	/**
	 * The name of the channel that the client is moved to. This is null in
	 * cases where the client is requesting to move to a channel, and is only
	 * provided by the server when it moves a client.
	 */
	private String channelName;

	public MoveToChannel(UUID channelId) {
		this.id = channelId;
	}

	@Override
	public int getByteCount() {
		return UUID_BYTES + getByteSize(this.channelName);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeUUID(this.id, o);
		writeString(this.channelName, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.id = readUUID(i);
		this.channelName = readString(i);
	}
}
