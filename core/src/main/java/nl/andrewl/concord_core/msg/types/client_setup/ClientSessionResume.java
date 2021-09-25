package nl.andrewl.concord_core.msg.types.client_setup;

import nl.andrewl.concord_core.msg.Message;

/**
 * This message is sent by the client to log into a server using a session token
 * instead of a username/password combination.
 */
public record ClientSessionResume(String sessionToken) implements Message {}
