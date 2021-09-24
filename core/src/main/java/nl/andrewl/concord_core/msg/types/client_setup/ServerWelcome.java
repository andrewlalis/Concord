package nl.andrewl.concord_core.msg.types.client_setup;

import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.ServerMetaData;

import java.util.UUID;

/**
 * This message is sent from the server to the client after the server accepts
 * the client's identification and registers the client in the server.
 *
 * @param clientId The unique id of this client.
 * @param sessionToken The token which this client can use to reconnect to the
 *                     server later and still be recognized as the same user.
 * @param currentChannelId The id of the channel that the user is placed in.
 * @param currentChannelName The name of the channel that the user is placed in.
 * @param metaData Information about the server's structure.
 */
public record ServerWelcome (
		UUID clientId,
		String sessionToken,
		UUID currentChannelId,
		String currentChannelName,
		ServerMetaData metaData
) implements Message {}
