package nl.andrewl.concord_server;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.andrewl.concord_core.msg.types.Identification;
import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_core.msg.types.ServerWelcome;
import nl.andrewl.concord_server.config.ServerConfig;
import org.dizitart.no2.IndexOptions;
import org.dizitart.no2.IndexType;
import org.dizitart.no2.Nitrite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * The main server implementation, which handles accepting new clients.
 */
public class ConcordServer implements Runnable {
	private final Map<UUID, ClientThread> clients;
	private volatile boolean running;

	@Getter
	private final ServerConfig config;
	@Getter
	private final IdProvider idProvider;
	@Getter
	private final Nitrite db;
	@Getter
	private final ExecutorService executorService;
	@Getter
	private final EventManager eventManager;
	@Getter
	private final ChannelManager channelManager;

	public ConcordServer() {
		this.idProvider = new UUIDProvider();
		this.config = ServerConfig.loadOrCreate(Path.of("server-config.json"), idProvider);
		this.db = Nitrite.builder()
				.filePath("concord-server.db")
				.openOrCreate();
		this.clients = new ConcurrentHashMap<>(32);

		this.executorService = Executors.newCachedThreadPool();
		this.eventManager = new EventManager(this);
		this.channelManager = new ChannelManager(this);
		for (var channelConfig : config.channels()) {
			this.channelManager.addChannel(new Channel(
					this,
					UUID.fromString(channelConfig.id()),
					channelConfig.name(),
					this.db.getCollection("channel-" + channelConfig.id())
			));
		}
		this.initDatabase();
	}

	private void initDatabase() {
		for (var channel : this.channelManager.getChannels()) {
			var col = channel.getMessageCollection();
			if (!col.hasIndex("timestamp")) {
				System.out.println("Adding timestamp index to collection for channel " + channel.getName());
				col.createIndex("timestamp", IndexOptions.indexOptions(IndexType.NonUnique));
			}
			if (!col.hasIndex("senderNickname")) {
				System.out.println("Adding senderNickname index to collection for channel " + channel.getName());
				col.createIndex("senderNickname", IndexOptions.indexOptions(IndexType.Fulltext));
			}
			if (!col.hasIndex("message")) {
				System.out.println("Adding message index to collection for channel " + channel.getName());
				col.createIndex("message", IndexOptions.indexOptions(IndexType.Fulltext));
			}
		}
	}

	/**
	 * Registers a new client as connected to the server. This is done once the
	 * client thread has received the correct identification information from
	 * the client. The server will register the client in its global set of
	 * connected clients, and it will immediately move the client to the default
	 * channel.
	 * @param identification The client's identification data.
	 * @param clientThread The client manager thread.
	 */
	public void registerClient(Identification identification, ClientThread clientThread) {
		var id = this.idProvider.newId();
		System.out.printf("Client \"%s\" joined with id %s.\n", identification.getNickname(), id);
		this.clients.put(id, clientThread);
		clientThread.setClientId(id);
		clientThread.setClientNickname(identification.getNickname());
		// Send a welcome reply containing all the initial server info the client needs.
		ServerMetaData metaData = new ServerMetaData(
				this.config.name(),
				this.channelManager.getChannels().stream()
						.map(channel -> new ServerMetaData.ChannelData(channel.getId(), channel.getName()))
						.sorted(Comparator.comparing(ServerMetaData.ChannelData::getName))
						.collect(Collectors.toList())
		);
		// Immediately add the client to the default channel and send the initial welcome message.
		var defaultChannel = this.channelManager.getChannelByName("general").orElseThrow();
		clientThread.sendToClient(new ServerWelcome(id, defaultChannel.getId(), metaData));
		// It is important that we send the welcome message first. The client expects this as the initial response to their identification message.
		defaultChannel.addClient(clientThread);
		clientThread.setCurrentChannel(defaultChannel);
		System.out.println("Moved client " + clientThread + " to " + defaultChannel);
	}

	/**
	 * De-registers a client from the server, removing them from any channel
	 * they're currently in.
	 * @param clientId The id of the client to remove.
	 */
	public void deregisterClient(UUID clientId) {
		var client = this.clients.remove(clientId);
		if (client != null) {
			client.getCurrentChannel().removeClient(client);
			client.shutdown();
			System.out.println("Client " + client + " has disconnected.");
		}
	}

	@Override
	public void run() {
		this.running = true;
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(this.config.port());
			StringBuilder startupMessage = new StringBuilder();
			startupMessage.append("Opened server on port ").append(config.port()).append("\n");
			for (var channel : this.channelManager.getChannels()) {
				startupMessage.append("\tChannel \"").append(channel).append('\n');
			}
			System.out.println(startupMessage);
			while (this.running) {
				Socket socket = serverSocket.accept();
				ClientThread clientThread = new ClientThread(socket, this);
				clientThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		var server = new ConcordServer();
		server.run();
	}
}
