package nl.andrewl.concord_core.msg.types.client_setup;

import nl.andrewl.concord_core.msg.Message;

/**
 * This message is sent by clients to log into a server that they have already
 * registered with, but don't have a valid session token for.
 */
public record ClientLogin(String username, String password) implements Message {}
