package nl.andrewl.concord_client.data;

import java.io.IOException;
import java.util.Optional;

/**
 * A component which can store and retrieve persistent data which a client can
 * use as part of its interaction with servers.
 */
public interface ClientDataStore {
	Optional<String> getSessionToken(String serverName) throws IOException;
	void saveSessionToken(String serverName, String sessionToken) throws IOException;
}
