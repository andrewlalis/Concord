package nl.andrewl.concord_core.msg.types.client_setup;

import nl.andrewl.concord_core.msg.Message;

/**
 * This message is sent from the client to a server, to provide identification
 * information about the client to the server when the connection is started.
 *
 * @param nickname
 */
public record Identification(String nickname, String sessionToken) implements Message {}
