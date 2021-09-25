package nl.andrewl.concord_core.msg.types.client_setup;

import nl.andrewl.concord_core.msg.Message;

/**
 * The data that new users should send to a server in order to register in that
 * server.
 */
public record ClientRegistration(
		String name,
		String description,
		String username,
		String password
) implements Message {}
