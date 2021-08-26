package nl.andrewl.concord_server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
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

	// Components for communicating with discovery servers.
	private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper mapper = new ObjectMapper();

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
		for (var channelConfig : config.getChannels()) {
			this.channelManager.addChannel(new Channel(
					this,
					UUID.fromString(channelConfig.getId()),
					channelConfig.getName(),
					this.db.getCollection("channel-" + channelConfig.getId())
			));
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

	public boolean isRunning() {
		return running;
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

	private void publishMetaDataToDiscoveryServers() {
		if (this.config.getDiscoveryServers().isEmpty()) return;
		ObjectNode node = this.mapper.createObjectNode();
		node.put("name", this.config.getName());
		node.put("description", this.config.getDescription());
		node.put("port", this.config.getPort());
		String json;
		try {
			json = this.mapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new UncheckedIOException(e);
		}
		var discoveryServers = List.copyOf(this.config.getDiscoveryServers());
		for (var discoveryServer : discoveryServers) {
			System.out.println("Publishing this server's metadata to discovery server at " + discoveryServer);
			var request = HttpRequest.newBuilder(URI.create(discoveryServer))
					.POST(HttpRequest.BodyPublishers.ofString(json))
					.header("Content-Type", "application/json")
					.timeout(Duration.ofSeconds(3))
					.build();
			try {
				this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		this.running = true;
		this.scheduledExecutorService.scheduleAtFixedRate(this::publishMetaDataToDiscoveryServers, 1, 1, TimeUnit.MINUTES);
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(this.config.getPort());
			StringBuilder startupMessage = new StringBuilder();
			startupMessage.append("Opened server on port ").append(config.getPort()).append("\n");
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
		this.scheduledExecutorService.shutdown();
	}

	public static void main(String[] args) {
		var server = new ConcordServer();
		new Thread(server).start();
		new ServerCli(server).run();
	}
}
