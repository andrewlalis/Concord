package nl.andrewl.concord_server.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_server.util.IdProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class ServerConfig {
	private String name;
	private String description;
	private int port;
	private boolean acceptAllNewClients;
	private int chatHistoryMaxCount;
	private int chatHistoryDefaultCount;
	private int maxMessageLength;
	private String defaultChannel;
	private List<ChannelConfig> channels;

	private List<String> discoveryServers;

	/**
	 * The path at which this config is stored.
	 */
	@JsonIgnore
	private transient Path filePath;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static final class ChannelConfig {
		private String id;
		private String name;
		private String description;
	}

	public static ServerConfig loadOrCreate(Path filePath, IdProvider idProvider) {
		ObjectMapper mapper = new ObjectMapper();
		ServerConfig config;
		if (Files.notExists(filePath)) {
			config = new ServerConfig(
					"My Concord Server",
					"A concord server for my friends and I.",
					8123,
					false,
					100,
					50,
					8192,
					"general",
					List.of(new ChannelConfig(idProvider.newId().toString(), "general", "Default channel for general discussion.")),
					List.of(),
					filePath
			);
			try (var out = Files.newOutputStream(filePath)) {
				mapper.writerWithDefaultPrettyPrinter().writeValue(out, config);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			System.err.println(filePath + " does not exist. Creating it with initial values. Edit and restart to apply changes.");
		} else {
			try {
				config = mapper.readValue(Files.newInputStream(filePath), ServerConfig.class);
				config.setFilePath(filePath);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			System.out.println("Loaded configuration from " + filePath);
		}
		return config;
	}

	public void save() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		try (var out = Files.newOutputStream(filePath)) {
			mapper.writerWithDefaultPrettyPrinter().writeValue(out, this);
		}
	}
}
