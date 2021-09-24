package nl.andrewl.concord_client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import lombok.Getter;
import nl.andrewl.concord_client.event.EventManager;
import nl.andrewl.concord_client.event.handlers.ChannelMovedHandler;
import nl.andrewl.concord_client.event.handlers.ChatHistoryResponseHandler;
import nl.andrewl.concord_client.event.handlers.ServerMetaDataHandler;
import nl.andrewl.concord_client.event.handlers.ServerUsersHandler;
import nl.andrewl.concord_client.gui.MainWindow;
import nl.andrewl.concord_client.model.ClientModel;
import nl.andrewl.concord_core.msg.Encryption;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.*;
import nl.andrewl.concord_core.msg.types.channel.MoveToChannel;
import nl.andrewl.concord_core.msg.types.chat.Chat;
import nl.andrewl.concord_core.msg.types.chat.ChatHistoryRequest;
import nl.andrewl.concord_core.msg.types.chat.ChatHistoryResponse;
import nl.andrewl.concord_core.msg.types.client_setup.Identification;
import nl.andrewl.concord_core.msg.types.client_setup.ServerWelcome;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConcordClient implements Runnable {
	private final Socket socket;
	private final InputStream in;
	private final OutputStream out;
	private final Serializer serializer;

	@Getter
	private final ClientModel model;

	private final EventManager eventManager;

	private volatile boolean running;

	public ConcordClient(String host, int port, String nickname, Path tokensFile) throws IOException {
		this.eventManager = new EventManager(this);
		this.socket = new Socket(host, port);
		this.serializer = new Serializer();
		try {
			var streams = Encryption.upgrade(socket.getInputStream(), socket.getOutputStream(), this.serializer);
			this.in = streams.first();
			this.out = streams.second();
		} catch (GeneralSecurityException e) {
			throw new IOException("Could not establish secure connection to the server.", e);
		}
		this.model = this.initializeConnectionToServer(nickname, tokensFile);

		// Add event listeners.
		this.eventManager.addHandler(MoveToChannel.class, new ChannelMovedHandler());
		this.eventManager.addHandler(ServerUsers.class, new ServerUsersHandler());
		this.eventManager.addHandler(ChatHistoryResponse.class, new ChatHistoryResponseHandler());
		this.eventManager.addHandler(Chat.class, (msg, client) -> client.getModel().getChatHistory().addChat(msg));
		this.eventManager.addHandler(ServerMetaData.class, new ServerMetaDataHandler());
	}

	/**
	 * Initializes the communication with the server by sending an {@link Identification}
	 * message, and waiting for a {@link ServerWelcome} response from the
	 * server. After that, we request some information about the channel we were
	 * placed in by the server.
	 * @param nickname The nickname to send to the server that it should know
	 *                 us by.
	 * @param tokensFile Path to the file where session tokens are stored.
	 * @return The client model that contains the server's metadata and other
	 * information that should be kept up-to-date at runtime.
	 * @throws IOException If an error occurs while reading or writing the
	 * messages, or if the server sends an unexpected response.
	 */
	private ClientModel initializeConnectionToServer(String nickname, Path tokensFile) throws IOException {
		String token = this.getSessionToken(tokensFile);
		this.serializer.writeMessage(new Identification(nickname, token), this.out);
		Message reply = this.serializer.readMessage(this.in);
		if (reply instanceof ServerWelcome welcome) {
			var model = new ClientModel(welcome.clientId(), nickname, welcome.currentChannelId(), welcome.currentChannelName(), welcome.metaData());
			this.saveSessionToken(welcome.sessionToken(), tokensFile);
			// Start fetching initial data for the channel we were initially put into.
			this.sendMessage(new ChatHistoryRequest(model.getCurrentChannelId(), ""));
			return model;
		} else {
			throw new IOException("Unexpected response from the server after sending identification message: " + reply);
		}
	}

	public void sendMessage(Message message) throws IOException {
		this.serializer.writeMessage(message, this.out);
	}

	public void sendChat(String message) throws IOException {
		this.serializer.writeMessage(new Chat(this.model.getId(), this.model.getNickname(), System.currentTimeMillis(), message), this.out);
	}

	public void shutdown() {
		this.running = false;
		if (!this.socket.isClosed()) {
			try {
				this.socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void run() {
		this.running = true;
		while (this.running) {
			try {
				Message msg = this.serializer.readMessage(this.in);
				this.eventManager.handle(msg);
			} catch (IOException e) {
				e.printStackTrace();
//				this.running = false;
			}
		}
		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Fetches the session token that this client should use for its currently
	 * configured server, according to the socket address and port.
	 * @param tokensFile The file containing the session tokens.
	 * @return The session token, or null if none was found.
	 * @throws IOException If the tokens file could not be read.
	 */
	@SuppressWarnings("unchecked")
	private String getSessionToken(Path tokensFile) throws IOException {
		String token = null;
		String address = this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort();
		if (Files.exists(tokensFile)) {
			ObjectMapper mapper = new ObjectMapper();
			Map<String, String> sessionTokens = mapper.readValue(Files.newBufferedReader(tokensFile), Map.class);
			token = sessionTokens.get(address);
		}
		return token;
	}

	/**
	 * Saves a session token that this client should use the next time it
	 * connects to the same server.
	 * @param token The token to save.
	 * @param tokensFile The file containing the session tokens.
	 * @throws IOException If the tokens file could not be read or written to.
	 */
	@SuppressWarnings("unchecked")
	private void saveSessionToken(String token, Path tokensFile) throws IOException {
		String address = this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort();
		Map<String, String> tokens = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();
		if (Files.exists(tokensFile)) {
			tokens = mapper.readValue(Files.newBufferedReader(tokensFile), Map.class);
		}
		tokens.put(address, token);
		mapper.writerWithDefaultPrettyPrinter().writeValue(Files.newBufferedWriter(tokensFile), tokens);
	}



	public static void main(String[] args) throws IOException {
		Terminal term = new DefaultTerminalFactory().createTerminal();
		Screen screen = new TerminalScreen(term);
		WindowBasedTextGUI gui = new MultiWindowTextGUI(screen);
		screen.startScreen();

		Window window = new MainWindow();
		window.setHints(List.of(Window.Hint.FULL_SCREEN));
		gui.addWindow(window);

		window.waitUntilClosed();
		screen.stopScreen();
	}
}
