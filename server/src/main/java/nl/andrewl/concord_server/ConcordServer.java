package nl.andrewl.concord_server;

import lombok.Getter;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.Identification;
import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_core.msg.types.ServerWelcome;
import nl.andrewl.concord_core.msg.types.UserData;
import nl.andrewl.concord_server.cli.ServerCli;
import nl.andrewl.concord_server.config.ServerConfig;
import org.dizitart.no2.Nitrite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * The main server implementation, which handles accepting new clients.
 */
public class ConcordServer implements Runnable {
	private static final Path CONFIG_FILE = Path.of("server-config.json");
	private static final Path DATABASE_FILE = Path.of("concord-server.db");

	private final Map<UUID, ClientThread> clients;
	private volatile boolean running;
	private final ServerSocket serverSocket;

	/**
	 * Server configuration data. This is used to define channels, discovery
	 * server addresses, and more.
	 */
	@Getter
	private final ServerConfig config;

	/**
	 * The component that generates new user and channel ids.
	 */
	@Getter
	private final IdProvider idProvider;

	/**
	 * The database that contains all messages and other server information.
	 */
	@Getter
	private final Nitrite db;

	/**
	 * A general-purpose executor service that can be used to submit async tasks.
	 */
	@Getter
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	/**
	 * Manager that handles incoming messages and events by clients.
	 */
	@Getter
	private final EventManager eventManager;

	/**
	 * Manager that handles the collection of channels in this server.
	 */
	@Getter
	private final ChannelManager channelManager;

	private final DiscoveryServerPublisher discoveryServerPublisher;
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

	public ConcordServer() throws IOException {
		this.idProvider = new UUIDProvider();
		this.config = ServerConfig.loadOrCreate(CONFIG_FILE, idProvider);
		this.discoveryServerPublisher = new DiscoveryServerPublisher(this.config);
		this.db = Nitrite.builder().filePath(DATABASE_FILE.toFile()).openOrCreate();
		this.clients = new ConcurrentHashMap<>(32);
		this.eventManager = new EventManager(this);
		this.channelManager = new ChannelManager(this);
		this.serverSocket = new ServerSocket(this.config.getPort());
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
		// Immediately add the client to the default channel and send the initial welcome message.
		var defaultChannel = this.channelManager.getDefaultChannel().orElseThrow();
		clientThread.sendToClient(new ServerWelcome(id, defaultChannel.getId(), this.getMetaData()));
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

	/**
	 * @return True if the server is currently running, meaning it is accepting
	 * connections, or false otherwise.
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Stops the server. Has no effect if the server has not started yet or has
	 * already been stopped.
	 */
	public void stop() {
		this.running = false;
		if (!this.serverSocket.isClosed()) {
			try {
				this.serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public List<UserData> getClients() {
		return this.clients.values().stream()
				.sorted(Comparator.comparing(ClientThread::getClientNickname))
				.map(ClientThread::toData)
				.collect(Collectors.toList());
	}

	public ServerMetaData getMetaData() {
		return new ServerMetaData(
				this.config.getName(),
				this.channelManager.getChannels().stream()
						.map(channel -> new ServerMetaData.ChannelData(channel.getId(), channel.getName()))
						.sorted(Comparator.comparing(ServerMetaData.ChannelData::getName))
						.collect(Collectors.toList())
		);
	}

	/**
	 * Sends a message to every connected client.
	 * @param message The message to send.
	 */
	public void broadcast(Message message) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(message.getByteCount());
		try {
			Serializer.writeMessage(message, baos);
			byte[] data = baos.toByteArray();
			for (var client : this.clients.values()) {
				client.sendToClient(data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shuts down the server cleanly by doing the following things:
	 * <ol>
	 *     <li>Disconnecting all clients.</li>
	 *     <li>Shutting down any executor services.</li>
	 *     <li>Flushing and compacting the message database.</li>
	 *     <li>Flushing the server configuration one last time.</li>
	 * </ol>
	 */
	private void shutdown() {
		System.out.println("Shutting down the server.");
		for (var clientId : this.clients.keySet()) {
			this.deregisterClient(clientId);
		}
		this.scheduledExecutorService.shutdown();
		this.executorService.shutdown();
		this.db.close();
		try {
			this.config.save();
		} catch (IOException e) {
			System.err.println("Could not save configuration on shutdown: " + e.getMessage());
		}
	}

	@Override
	public void run() {
		this.running = true;
		this.scheduledExecutorService.scheduleAtFixedRate(this.discoveryServerPublisher::publish, 0, 1, TimeUnit.MINUTES);
		StringBuilder startupMessage = new StringBuilder();
		startupMessage.append("Opened server on port ").append(config.getPort()).append("\n")
				.append("The following channels are available:\n");
		for (var channel : this.channelManager.getChannels()) {
			startupMessage.append("\tChannel \"").append(channel).append('\n');
		}
		System.out.println(startupMessage);
		while (this.running) {
			try {
				Socket socket = this.serverSocket.accept();
				ClientThread clientThread = new ClientThread(socket, this);
				clientThread.start();
			} catch (IOException e) {
				System.err.println("Could not accept new client connection: " + e.getMessage());
			}
		}
		this.shutdown();
	}

	public static void main(String[] args) throws IOException {
		var server = new ConcordServer();
		new Thread(server).start();
		new ServerCli(server).run();
	}
}
