package nl.andrewl.concord_server;

import nl.andrewl.concord_core.msg.types.MoveToChannel;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager {
	private final ConcordServer server;
	private final Map<String, Channel> channelNameMap;
	private final Map<UUID, Channel> channelIdMap;

	public ChannelManager(ConcordServer server) {
		this.server = server;
		this.channelNameMap = new ConcurrentHashMap<>();
		this.channelIdMap = new ConcurrentHashMap<>();
		// Initialize the channels according to what's defined in the server's config.
		for (var channelConfig : server.getConfig().getChannels()) {
			this.addChannel(new Channel(
					server,
					UUID.fromString(channelConfig.getId()),
					channelConfig.getName(),
					server.getDb().getCollection("channel-" + channelConfig.getId())
			));
		}
	}

	public Set<Channel> getChannels() {
		return Set.copyOf(this.channelIdMap.values());
	}

	public Optional<Channel> getDefaultChannel() {
		var optionalGeneral = this.getChannelByName("general");
		if (optionalGeneral.isPresent()) {
			return optionalGeneral;
		}
		for (var channel : this.getChannels()) {
			return Optional.of(channel);
		}
		return Optional.empty();
	}

	public void addChannel(Channel channel) {
		this.channelNameMap.put(channel.getName(), channel);
		this.channelIdMap.put(channel.getId(), channel);
	}

	public void removeChannel(Channel channel) {
		this.channelNameMap.remove(channel.getName());
		this.channelIdMap.remove(channel.getId());
	}

	public Optional<Channel> getChannelByName(String name) {
		return Optional.ofNullable(this.channelNameMap.get(name));
	}

	public Optional<Channel> getChannelById(UUID id) {
		return Optional.ofNullable(this.channelIdMap.get(id));
	}

	public void moveToChannel(ClientThread client, Channel channel) {
		if (client.getCurrentChannel() != null) {
			var previousChannel = client.getCurrentChannel();
			previousChannel.removeClient(client);
		}
		channel.addClient(client);
		client.setCurrentChannel(channel);
		client.sendToClient(new MoveToChannel(channel.getId()));
		System.out.println("Moved client " + client + " to channel " + channel);
	}
}
