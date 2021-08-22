package nl.andrewl.concord_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import nl.andrewl.concord_server.IdProvider;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Log
public record ServerConfig(
		String name,
		int port,
		ChannelConfig[] channels
) {

	public static record ChannelConfig (
			String id,
			String name,
			String description
	) {}

	public static ServerConfig loadOrCreate(Path filePath, IdProvider idProvider) {
		ObjectMapper mapper = new ObjectMapper();
		ServerConfig config;
		if (Files.notExists(filePath)) {
			config = new ServerConfig(
					"My Concord Server",
					8123,
					new ServerConfig.ChannelConfig[]{
							new ServerConfig.ChannelConfig(idProvider.newId().toString(), "general", "Default channel for general discussion.")
					}
			);
			try (var out = Files.newOutputStream(filePath)) {
				mapper.writerWithDefaultPrettyPrinter().writeValue(out, config);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			log.info(filePath + " does not exist. Creating it with initial values. Edit and restart to apply changes.");
		} else {
			try {
				config = mapper.readValue(Files.newInputStream(filePath), ServerConfig.class);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			log.info("Loaded configuration from " + filePath);
		}
		return config;
	}
}
