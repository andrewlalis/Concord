package nl.andrewl.concord_server.client;

import at.favre.lib.crypto.bcrypt.BCrypt;
import nl.andrewl.concord_core.msg.types.client_setup.ClientLogin;
import nl.andrewl.concord_core.msg.types.client_setup.ClientRegistration;
import nl.andrewl.concord_core.msg.types.client_setup.ClientSessionResume;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.util.CollectionUtils;
import nl.andrewl.concord_server.util.StringUtils;
import org.dizitart.no2.Document;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.filters.Filters;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * This authentication service provides support for managing the client's
 * authentication status, such as registering new clients, generating tokens,
 * and logging in.
 */
public class AuthenticationService {
	public static record ClientConnectionData(UUID id, String nickname, String sessionToken, boolean newClient) {}

	private final NitriteCollection userCollection;
	private final NitriteCollection sessionTokenCollection;
	private final ConcordServer server;

	public AuthenticationService(ConcordServer server, NitriteCollection userCollection) {
		this.server = server;
		this.userCollection = userCollection;
		this.sessionTokenCollection = server.getDb().getCollection("session-tokens");
		CollectionUtils.ensureIndexes(this.sessionTokenCollection, Map.of(
				"sessionToken", IndexType.Unique,
				"userId", IndexType.NonUnique,
				"expiresAt", IndexType.NonUnique
		));
	}

	public ClientConnectionData registerNewClient(ClientRegistration registration) {
		UUID id = this.server.getIdProvider().newId();
		String sessionToken = this.generateSessionToken(id);
		String passwordHash = BCrypt.withDefaults().hashToString(12, registration.password().toCharArray());
		Document doc = new Document(Map.of(
				"id", id,
				"username", registration.username(),
				"passwordHash", passwordHash,
				"name", registration.name(),
				"description", registration.description(),
				"createdAt", System.currentTimeMillis(),
				"pending", false
		));
		this.userCollection.insert(doc);
		return new ClientConnectionData(id, registration.username(), sessionToken, true);
	}

	public UUID registerPendingClient(ClientRegistration registration) {
		UUID id = this.server.getIdProvider().newId();
		String passwordHash = BCrypt.withDefaults().hashToString(12, registration.password().toCharArray());
		Document doc = new Document(Map.of(
				"id", id,
				"username", registration.username(),
				"passwordHash", passwordHash,
				"name", registration.name(),
				"description", registration.description(),
				"createdAt", System.currentTimeMillis(),
				"pending", true
		));
		this.userCollection.insert(doc);
		return id;
	}

	public Document findAndAuthenticateUser(ClientLogin login) {
		Document userDoc = this.userCollection.find(Filters.eq("username", login.username())).firstOrDefault();
		if (userDoc != null) {
			byte[] passwordHash = userDoc.get("passwordHash", String.class).getBytes(StandardCharsets.UTF_8);
			if (BCrypt.verifyer().verify(login.password().getBytes(StandardCharsets.UTF_8), passwordHash).verified) {
				return userDoc;
			}
		}
		return null;
	}

	public Document findAndAuthenticateUser(ClientSessionResume sessionResume) {
		Document tokenDoc = this.sessionTokenCollection.find(Filters.and(
				Filters.eq("sessionToken", sessionResume.sessionToken()),
				Filters.gt("expiresAt", Instant.now().toEpochMilli())
		)).firstOrDefault();
		if (tokenDoc == null) return null;
		UUID userId = tokenDoc.get("userId", UUID.class);
		return this.userCollection.find(Filters.eq("id", userId)).firstOrDefault();
	}

	public String generateSessionToken(UUID userId) {
		String sessionToken = StringUtils.random(128);
		long expiresAt = Instant.now().plus(7, ChronoUnit.DAYS).toEpochMilli();
		Document doc = new Document(Map.of(
				"sessionToken", sessionToken,
				"userId", userId,
				"expiresAt", expiresAt
		));
		this.sessionTokenCollection.insert(doc);
		return sessionToken;
	}

	public void removeExpiredSessionTokens() {
		long now = System.currentTimeMillis();
		this.sessionTokenCollection.remove(Filters.lt("expiresAt", now));
	}
}
