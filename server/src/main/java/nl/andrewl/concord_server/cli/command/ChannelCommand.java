package nl.andrewl.concord_server.cli.command;

import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.channel.Channel;
import nl.andrewl.concord_server.cli.ServerCliCommand;
import nl.andrewl.concord_server.config.ServerConfig;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Command for interacting with channels on the server.
 */
public class ChannelCommand implements ServerCliCommand {
	@Override
	public void handle(ConcordServer server, String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Missing required subcommand. Valid subcommands are: add, remove, list");
			return;
		}
		String subcommand = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);
		switch (subcommand) {
			case "add" -> addChannel(server, args);
			case "remove" -> removeChannel(server, args);
			case "list" -> listChannels(server);
			default -> System.err.println("Unknown subcommand.");
		}
	}

	private void addChannel(ConcordServer server, String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("Missing required name argument.");
			return;
		}
		String name = args[0].trim().toLowerCase().replaceAll("\\s+", "-");
		if (name.isBlank()) {
			System.err.println("Cannot create channel with blank name.");
			return;
		}
		if (server.getChannelManager().getChannelByName(name).isPresent()) {
			System.err.println("Channel with that name already exists.");
			return;
		}
		String description = null;
		if (args.length > 1) {
			description = args[1].trim();
		}
		UUID id = server.getIdProvider().newId();
		var channelConfig = new ServerConfig.ChannelConfig(id.toString(), name, description);
		server.getConfig().getChannels().add(channelConfig);
		server.getConfig().save();

		var channel = new Channel(server, id, name);
		server.getChannelManager().addChannel(channel);
		server.getClientManager().broadcast(server.getMetaData());
		System.out.println("Added channel " + channel.getAsTag() + ".");
	}

	private void removeChannel(ConcordServer server, String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Missing required channel name.");
			return;
		}
		String name = args[0].trim().toLowerCase();
		Optional<Channel> optionalChannel = server.getChannelManager().getChannelByName(name);
		if (optionalChannel.isEmpty()) {
			System.err.println("No channel with that name exists.");
			return;
		}
		Channel channelToRemove = optionalChannel.get();
		Channel alternative = null;
		for (var c : server.getChannelManager().getChannels()) {
			if (!c.equals(channelToRemove)) {
				alternative = c;
				break;
			}
		}
		if (alternative == null) {
			System.err.println("No alternative channel could be found. A server must always have at least one channel.");
			return;
		}
		for (var client : channelToRemove.getConnectedClients()) {
			server.getChannelManager().moveToChannel(client, alternative);
		}
		server.getChannelManager().removeChannel(channelToRemove);
		server.getDb().getContext().dropCollection(channelToRemove.getMessageCollection().getName());
		server.getConfig().getChannels().removeIf(channelConfig -> channelConfig.getName().equals(channelToRemove.getName()));
		server.getConfig().save();
		server.getClientManager().broadcast(server.getMetaData());
		System.out.println("Removed the channel " + channelToRemove);
	}

	private void listChannels(ConcordServer server) {
		StringBuilder sb = new StringBuilder();
		server.getChannelManager().getChannels().stream().sorted()
				.forEachOrdered(channel -> sb.append(channel.getAsTag())
						.append(" - ").append(channel.getConnectedClients().size())
						.append(" users").append("\n")
				);
		System.out.print(sb);
	}
}
