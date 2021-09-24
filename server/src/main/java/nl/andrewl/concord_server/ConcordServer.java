package nl.andrewl.concord_server;

import lombok.Getter;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_server.channel.ChannelManager;
import nl.andrewl.concord_server.cli.ServerCli;
import nl.andrewl.concord_server.client.ClientManager;
import nl.andrewl.concord_server.client.ClientThread;
import nl.andrewl.concord_server.config.ServerConfig;
import nl.andrewl.concord_server.event.EventManager;
import nl.andrewl.concord_server.util.IdProvider;
import nl.andrewl.concord_server.util.UUIDProvider;
import org.dizitart.no2.Nitrite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The main server implementation, which handles accepting new clients.
 */
public class ConcordServer implements Runnable {
	private static final Path CONFIG_FILE = Path.of("server-config.json");
	private static final Path DATABASE_FILE = Path.of("concord-server.db");

	private volatile boolean running;
	private final ServerSocket serverSocket;

	/**
	 * A utility serializer that's mostly used when preparing a message to
	 * broadcast to a set of users, which is more efficient than having each
	 * individual client thread serialize the same message before sending it.
	 */
	@Getter
	private final Serializer serializer;

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

	/**
	 * Manager that handles the collection of clients connected to this server.
	 */
	@Getter
	private final ClientManager clientManager;

	private final DiscoveryServerPublisher discoveryServerPublisher;
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

	public ConcordServer() throws IOException {
		this.idProvider = new UUIDProvider();
		this.config = ServerConfig.loadOrCreate(CONFIG_FILE, idProvider);
		this.discoveryServerPublisher = new DiscoveryServerPublisher(this.config);
		this.db = Nitrite.builder().filePath(DATABASE_FILE.toFile()).openOrCreate();
		this.eventManager = new EventManager(this);
		this.channelManager = new ChannelManager(this);
		this.clientManager = new ClientManager(this);
		this.serverSocket = new ServerSocket(this.config.getPort());
		this.serializer = new Serializer();
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

	public ServerMetaData getMetaData() {
		return new ServerMetaData(
				this.config.getName(),
				this.channelManager.getChannels().stream()
						.map(channel -> new ServerMetaData.ChannelData(channel.getId(), channel.getName()))
						.sorted(Comparator.comparing(ServerMetaData.ChannelData::name))
						.toList().toArray(new ServerMetaData.ChannelData[0])
		);
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
		for (var clientId : this.clientManager.getConnectedIds()) {
			this.clientManager.handleLogOut(clientId);
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
		System.out.printf("Opened server on port %d.\n", config.getPort());
		while (this.running) {
			try {
				Socket socket = this.serverSocket.accept();
				ClientThread clientThread = new ClientThread(socket, this);
				clientThread.start();
			} catch (IOException e) {
				if (!e.getMessage().equalsIgnoreCase("socket closed")) {
					System.err.println("Could not accept new client connection: " + e.getMessage());
				}
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
