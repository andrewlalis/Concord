package nl.andrewl.concord_core.msg.types.channel;

import nl.andrewl.concord_core.msg.Message;

import java.util.UUID;

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
 * @param id The id of the channel that the client is requesting or being moved
 *           to, or the id of another client that the user wishes to begin
 *           private messaging with.
 * @param channelName The name of the channel that the client is moved to. This
 *                    is null in cases where the client is requesting to move to
 *                    a channel, and is only provided by the server when it
 *                    moves a client.
 */
public record MoveToChannel (UUID id, String channelName) implements Message {
	public MoveToChannel(UUID id) {
		this(id, null);
	}
}
