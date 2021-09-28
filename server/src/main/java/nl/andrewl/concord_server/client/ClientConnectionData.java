package nl.andrewl.concord_server.client;

import java.util.UUID;

/**
 * Some common data that's used when dealing with a client who has just joined
 * the server.
 * @param id The user's unique id.
 * @param username The user's unique username.
 * @param sessionToken The user's new session token that can be used the next
 *                     time they want to log in.
 * @param newClient True if this client is connecting for the first time, or
 *                  false otherwise.
 */
public record ClientConnectionData(UUID id, String username, String sessionToken, boolean newClient) {}
