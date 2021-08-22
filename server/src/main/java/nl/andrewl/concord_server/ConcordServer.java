package nl.andrewl.concord_server;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.andrewl.concord_core.msg.types.Identification;
import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_core.msg.types.ServerWelcome;
import org.dizitart.no2.Nitrite;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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

	public ConcordServer(int port) {
		this.port = port;
		this.idProvider = new UUIDProvider();
		this.db = Nitrite.builder()
				.filePath("concord-server.db")
				.openOrCreate();
		this.clients = new ConcurrentHashMap<>(32);

		this.executorService = Executors.newCachedThreadPool();
		this.eventManager = new EventManager(this);
		this.channelManager = new ChannelManager(this);
	}

	/**
	 * Registers a new client as connected to the server. This is done once the
	 * client thread has received the correct identification information from
	 * the client. The server will register the client in its global set of
	 * connected clients, and it will immediately move the client to the default
	 * channel.
	 * @param identification The client's identification data.
	 * @param clientThread The client manager thread.
	 * @return The id of the client.
	 */
	public UUID registerClient(Identification identification, ClientThread clientThread) {
		var id = this.idProvider.newId();
		log.info("Registering new client " + identification.getNickname() + " with id " + id);
		this.clients.put(id, clientThread);
		// Send a welcome reply containing all the initial server info the client needs.
		ServerMetaData metaData = new ServerMetaData(
				"Testing Server",
				this.channelManager.getChannels().stream()
						.map(channel -> new ServerMetaData.ChannelData(channel.getId(), channel.getName()))
						.sorted(Comparator.comparing(ServerMetaData.ChannelData::getName))
						.collect(Collectors.toList())
		);
		var defaultChannel = this.channelManager.getChannelByName("general").orElseThrow();
		defaultChannel.addClient(clientThread);
		clientThread.setCurrentChannel(defaultChannel);
		clientThread.sendToClient(new ServerWelcome(id, defaultChannel.getId(), metaData));

		return id;
	}

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
		var server = new ConcordServer(8123);
		server.run();
	}
}
