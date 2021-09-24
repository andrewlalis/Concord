package nl.andrewl.concord_core.msg.types.chat;

import nl.andrewl.concord_core.msg.Message;

import java.util.UUID;

/**
 * The response that a server sends to a {@link ChatHistoryRequest}. The list of
 * messages is ordered by timestamp, with the newest messages appearing first.
 */
public record ChatHistoryResponse (UUID channelId, Chat[] messages) implements Message {}
