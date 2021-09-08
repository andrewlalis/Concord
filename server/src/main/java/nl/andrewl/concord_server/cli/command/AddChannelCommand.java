package nl.andrewl.concord_server.cli.command;

import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.channel.Channel;
import nl.andrewl.concord_server.cli.ServerCliCommand;
import nl.andrewl.concord_server.config.ServerConfig;

import java.util.UUID;

/**
 * This command adds a new channel to the server.
 */
public class AddChannelCommand implements ServerCliCommand {
	@Override
	public void handle(ConcordServer server, String[] args) throws Exception {
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

		server.getChannelManager().addChannel(new Channel(server, id, name));
		server.getClientManager().broadcast(server.getMetaData());
	}
}
