package nl.andrewl.concord_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import nl.andrewl.concord_server.config.ServerConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * This component is responsible for publishing the server's metadata to any
 * discovery servers that have been defined in the server's configuration file.
 */
public class DiscoveryServerPublisher {
	private final ObjectMapper mapper = new ObjectMapper();
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ServerConfig config;

	public DiscoveryServerPublisher(ServerConfig config) {
		this.config = config;
	}

	public void publish() {
		if (this.config.getDiscoveryServers().isEmpty()) return;
		ObjectNode node = this.mapper.createObjectNode();
		node.put("name", this.config.getName());
		node.put("description", this.config.getDescription());
		node.put("port", this.config.getPort());
		String json;
		try {
			json = this.mapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
		var discoveryServers = List.copyOf(this.config.getDiscoveryServers());
		for (var discoveryServer : discoveryServers) {
			var request = HttpRequest.newBuilder(URI.create(discoveryServer))
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.header("Content-Type", "application/json")
					.timeout(Duration.ofSeconds(3))
					.build();
			try {
				this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
			} catch (IOException | InterruptedException e) {
				System.err.println("Could not publish metadata to " + discoveryServer + " because of exception: " + e.getClass().getSimpleName());
			}
		}
	}
}
