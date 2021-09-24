package nl.andrewl.concord_core.msg.types;

import nl.andrewl.concord_core.msg.Message;

import java.util.UUID;

/**
 * Metadata is sent by the server to clients to inform them of the structure of
 * the server. This includes basic information about the server's own properties
 * as well as information about all top-level channels.
 */
public record ServerMetaData (String name, ChannelData[] channels) implements Message {
	/**
	 * Metadata about a top-level channel in the server which is visible and
	 * joinable for a user.
	 */
	public static record ChannelData (UUID id, String name) implements Message {}
}
