package nl.andrewl.concord_server.channel;

import nl.andrewl.concord_core.msg.types.channel.MoveToChannel;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.client.ClientThread;
import nl.andrewl.concord_server.util.CollectionUtils;
import org.dizitart.no2.Document;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.NitriteCollection;
import org.dizitart.no2.filters.Filters;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This manager is responsible for keeping track of all the channels in the
 * server, and controlling modifications to them.
 */
public class ChannelManager {
	private final ConcordServer server;
	private final Map<String, Channel> channelNameMap;
	private final Map<UUID, Channel> channelIdMap;

	private final Map<Set<UUID>, Channel> privateChannels;
	private final NitriteCollection privateChannelCollection;

	public ChannelManager(ConcordServer server) {
		this.server = server;
		this.channelNameMap = new ConcurrentHashMap<>();
		this.channelIdMap = new ConcurrentHashMap<>();
		this.privateChannels = new ConcurrentHashMap<>();
		this.privateChannelCollection = this.server.getDb().getCollection("private-channels");
		CollectionUtils.ensureIndexes(this.privateChannelCollection, Map.of(
				"idHash", IndexType.Unique,
				"id", IndexType.Unique
		));
		// Initialize the channels according to what's defined in the server's config.
		for (var channelConfig : server.getConfig().getChannels()) {
			this.addChannel(new Channel(server, UUID.fromString(channelConfig.getId()), channelConfig.getName()));
		}
	}

	public Set<Channel> getChannels() {
		return Set.copyOf(this.channelIdMap.values());
	}

	public Optional<Channel> getDefaultChannel() {
		var optionalDefault = this.getChannelByName(this.server.getConfig().getDefaultChannel());
		if (optionalDefault.isPresent()) {
			return optionalDefault;
		}
		System.err.println("Could not find a channel with the name \"" + this.server.getConfig().getDefaultChannel() + "\".");
		for (var channel : this.getChannels()) {
			return Optional.of(channel);
		}
		System.err.println("Could not find any channel to use as a default channel.");
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

	/**
	 * Moves a client to the given channel. This involves removing the client
	 * from whatever channel they're currently in, if any, moving them to the
	 * new channel, and sending them a message to indicate that it has been done.
	 * @param client The client to move.
	 * @param channel The channel to move the client to.
	 */
	public void moveToChannel(ClientThread client, Channel channel) {
		if (client.getCurrentChannel() != null) {
			var previousChannel = client.getCurrentChannel();
			previousChannel.removeClient(client);
		}
		channel.addClient(client);
		client.setCurrentChannel(channel);
		client.sendToClient(new MoveToChannel(channel.getId(), channel.getName()));
	}

	/**
	 * Gets or creates a private channel for the given client ids to be able to
	 * communicate together. No other clients are allowed to access the channel.
	 * @param clientIds The id of each client which should have access to the
	 *                  channel.
	 * @return The private channel.
	 */
	public Channel getPrivateChannel(Set<UUID> clientIds) {
		if (clientIds.size() < 2) {
			throw new IllegalArgumentException("At least 2 client ids are required for a private channel.");
		}
		return this.privateChannels.computeIfAbsent(clientIds, this::getPrivateChannelFromDatabase);
	}

	/**
	 * Gets a private channel, given the id of a client who is part of the
	 * channel, and the id of the channel.
	 * @param clientId The id of the client that's requesting the channel.
	 * @param channelId The id of the private channel.
	 * @return The private channel.
	 */
	public Optional<Channel> getPrivateChannel(UUID clientId, UUID channelId) {
		Channel privateChannel = this.privateChannels.entrySet().stream()
				.filter(entry -> entry.getKey().contains(clientId) && entry.getValue().getId().equals(channelId))
				.findAny().map(Map.Entry::getValue).orElse(null);
		if (privateChannel == null) {
			var cursor = this.privateChannelCollection.find(Filters.and(Filters.eq("id", channelId), Filters.in("clientIds", clientId)));
			Document channelInfo = cursor.firstOrDefault();
			if (channelInfo != null) {
				privateChannel = new Channel(
						this.server,
						channelInfo.get("id", UUID.class),
						channelInfo.get("name", String.class)
				);
				Set<UUID> clientIds = Set.of(channelInfo.get("clientIds", UUID[].class));
				this.privateChannels.put(clientIds, privateChannel);
			}
		}
		return Optional.ofNullable(privateChannel);
	}

	/**
	 * Gets and instantiates a private channel from information stored in the
	 * "private-channels" collection of the database, or creates it if it does
	 * not exist yet.
	 * @param clientIds The set of client ids that the channel is for.
	 * @return The private channel.
	 */
	private Channel getPrivateChannelFromDatabase(Set<UUID> clientIds) {
		// First check if a private channel for these clients exists in the database.
		String idHash = clientIds.stream().sorted().map(UUID::toString).collect(Collectors.joining());
		var cursor = this.privateChannelCollection.find(Filters.eq("idHash", idHash));
		Document channelInfo = cursor.firstOrDefault();
		if (channelInfo != null) {
			// If it does exist, instantiate a channel with its info.
			return new Channel(
					this.server,
					channelInfo.get("id", UUID.class),
					channelInfo.get("name", String.class)
			);
		} else {
			// Otherwise, create the channel anew and save it in the collection.
			var channel = new Channel(this.server, this.server.getIdProvider().newId(), "Private Channel");
			channelInfo = new Document(Map.of(
					"idHash", idHash,
					"id", channel.getId(),
					"name", channel.getName(),
					"clientIds", clientIds.toArray(new UUID[0])
			));
			this.privateChannelCollection.insert(channelInfo);
			System.out.println("Created new private channel for clients: " + clientIds);
			return channel;
		}
	}
}
