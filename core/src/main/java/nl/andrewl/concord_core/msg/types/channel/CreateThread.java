package nl.andrewl.concord_core.msg.types.channel;

import nl.andrewl.concord_core.msg.Message;

import java.util.UUID;

/**
 * This message is sent by clients when they indicate that they would like to
 * create a new thread in their current channel.
 * <p>
 *     Conversely, this message is also sent by the server when a thread has
 *     been created by someone, and all clients need to be notified so that they
 *     can properly display to the user that a message has been turned into a
 *     thread.
 * </p>
 *
 * @param messageId The id of the message that a thread will be/is attached to.
 * @param title The title of the thread. This may be null.
 */
public record CreateThread (UUID messageId, String title) implements Message {}
