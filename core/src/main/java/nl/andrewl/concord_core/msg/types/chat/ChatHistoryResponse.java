package nl.andrewl.concord_core.msg.types.chat;

import nl.andrewl.concord_core.msg.Message;

import java.util.UUID;

/**
 * The response that a server sends to a {@link ChatHistoryRequest}. The list of
 * messages is ordered by timestamp, with the newest messages appearing first.
 * @param channelId The id of the channel that the chat messages belong to.
 * @param messages The list of messages that comprises the history.
 */
public record ChatHistoryResponse (UUID channelId, Chat[] messages) implements Message {}
