package nl.andrewl.concord_client.data;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class JsonClientDataStore implements ClientDataStore {
	private final Path file;

	public JsonClientDataStore(Path file) {
		this.file = file;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Optional<String> getSessionToken(String serverName) throws IOException {
		String token = null;
		if (Files.exists(file)) {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> sessionTokens = mapper.readValue(Files.newBufferedReader(file), Map.class);
			token = sessionTokens.get(serverName);
		}
		return Optional.ofNullable(token);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void saveSessionToken(String serverName, String sessionToken) throws IOException {
		Map<String, String> tokens = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		if (Files.exists(file)) {
			tokens = mapper.readValue(Files.newBufferedReader(file), Map.class);
		}
		tokens.put(serverName, sessionToken);
		mapper.writerWithDefaultPrettyPrinter().writeValue(Files.newBufferedWriter(file), tokens);
	}
}
