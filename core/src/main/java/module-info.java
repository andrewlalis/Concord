/**
 * The core components that are used by both the Concord server and the default
 * client implementation. Includes record-based message serialization, and some
 * utilities for message passing.
 * <p>
 *     This core module defines the message protocol that clients must use to
 *     communicate with any server.
 * </p>
 */
module concord_core {
	requires static lombok;

	exports nl.andrewl.concord_core.util to concord_server, concord_client;
	exports nl.andrewl.concord_core.msg to concord_server, concord_client;

	exports nl.andrewl.concord_core.msg.types to concord_server, concord_client;
	exports nl.andrewl.concord_core.msg.types.client_setup to concord_client, concord_server;
	exports nl.andrewl.concord_core.msg.types.chat to concord_client, concord_server;
	exports nl.andrewl.concord_core.msg.types.channel to concord_client, concord_server;
}