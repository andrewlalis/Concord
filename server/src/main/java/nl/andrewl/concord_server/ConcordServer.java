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

@Log
public class ConcordServer implements Runnable {
	private final Map<UUID, ClientThread> clients;
	private final int port;
	private final String name;
	@Getter
	private final IdProvider idProvider;
	@Getter
	private final Nitrite db;
	private volatile boolean running;
	@Getter
	private final ExecutorService executorService;
	@Getter
	private final EventManager eventManager;
	@Getter
	private final ChannelManager channelManager;

	public ConcordServer() {
		this.idProvider = new UUIDProvider();
		ServerConfig config = ServerConfig.loadOrCreate(Path.of("server-config.json"), idProvider);
		this.port = config.port();
		this.name = config.name();
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
				log.info("Adding timestamp index to collection for channel " + channel.getName());
				col.createIndex("timestamp", IndexOptions.indexOptions(IndexType.NonUnique));
			}
			if (!col.hasIndex("senderNickname")) {
				log.info("Adding senderNickname index to collection for channel " + channel.getName());
				col.createIndex("senderNickname", IndexOptions.indexOptions(IndexType.Fulltext));
			}
			if (!col.hasIndex("message")) {
				log.info("Adding message index to collection for channel " + channel.getName());
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
		log.info("Registering new client " + identification.getNickname() + " with id " + id);
		this.clients.put(id, clientThread);
		clientThread.setClientId(id);
		clientThread.setClientNickname(identification.getNickname());
		// Send a welcome reply containing all the initial server info the client needs.
		ServerMetaData metaData = new ServerMetaData(
				this.name,
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
		}
	}

	@Override
	public void run() {
		this.running = true;
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(this.port);
			log.info("Opened server on port " + this.port);
			while (this.running) {
				Socket socket = serverSocket.accept();
				log.info("Accepted new socket connection from " + socket.getInetAddress().getHostAddress());
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
