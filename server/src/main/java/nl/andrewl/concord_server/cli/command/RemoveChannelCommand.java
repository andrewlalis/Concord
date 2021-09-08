package nl.andrewl.concord_server.cli.command;

import nl.andrewl.concord_server.channel.Channel;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.cli.ServerCliCommand;

import java.util.Optional;

public class RemoveChannelCommand implements ServerCliCommand {
	@Override
	public void handle(ConcordServer server, String[] args) throws Exception {
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
}
