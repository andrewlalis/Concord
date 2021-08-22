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
	}

	public Set<Channel> getChannels() {
		return Set.copyOf(this.channelIdMap.values());
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
		System.out.println("Moved client " + client.getClientNickname() + " to channel " + channel.getName());
	}
}
