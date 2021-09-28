package nl.andrewl.concord_client;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import lombok.Getter;
import nl.andrewl.concord_client.data.ClientDataStore;
import nl.andrewl.concord_client.data.JsonClientDataStore;
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
import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_core.msg.types.ServerUsers;
import nl.andrewl.concord_core.msg.types.channel.MoveToChannel;
import nl.andrewl.concord_core.msg.types.chat.Chat;
import nl.andrewl.concord_core.msg.types.chat.ChatHistoryRequest;
import nl.andrewl.concord_core.msg.types.chat.ChatHistoryResponse;
import nl.andrewl.concord_core.msg.types.client_setup.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

public class ConcordClient implements Runnable {
	private final Socket socket;
	private final InputStream in;
	private final OutputStream out;
	private final Serializer serializer;
	private final ClientDataStore dataStore;

	@Getter
	private ClientModel model;

	private final EventManager eventManager;

	private volatile boolean running;

	private ConcordClient(String host, int port) throws IOException {
		this.eventManager = new EventManager(this);
		this.socket = new Socket(host, port);
		this.serializer = new Serializer();
		this.dataStore = new JsonClientDataStore(Path.of("concord-session-tokens.json"));
		try {
			var streams = Encryption.upgrade(socket.getInputStream(), socket.getOutputStream(), this.serializer);
			this.in = streams.first();
			this.out = streams.second();
		} catch (GeneralSecurityException e) {
			throw new IOException("Could not establish secure connection to the server.", e);
		}
		// Add event listeners.
		this.eventManager.addHandler(MoveToChannel.class, new ChannelMovedHandler());
		this.eventManager.addHandler(ServerUsers.class, new ServerUsersHandler());
		this.eventManager.addHandler(ChatHistoryResponse.class, new ChatHistoryResponseHandler());
		this.eventManager.addHandler(Chat.class, (msg, client) -> client.getModel().getChatHistory().addChat(msg));
		this.eventManager.addHandler(ServerMetaData.class, new ServerMetaDataHandler());
	}

	public static ConcordClient register(String host, int port, String username, String password) throws IOException {
		var client = new ConcordClient(host, port);
		client.sendMessage(new ClientRegistration(null, null, username, password));
		Message reply = client.serializer.readMessage(client.in);
		if (reply instanceof RegistrationStatus status) {
			if (status.type() == RegistrationStatus.Type.ACCEPTED) {
				ServerWelcome welcomeData = (ServerWelcome) client.serializer.readMessage(client.in);
				client.initializeClientModel(welcomeData, username);
			} else if (status.type() == RegistrationStatus.Type.PENDING) {
				System.out.println("Registration pending!");
			}
		} else {
			System.out.println(reply);
		}
		return client;
	}

	public static ConcordClient login(String host, int port, String username, String password) throws IOException {
		var client = new ConcordClient(host, port);
		client.sendMessage(new ClientLogin(username, password));
		Message reply = client.serializer.readMessage(client.in);
		if (reply instanceof ServerWelcome welcome) {
			client.initializeClientModel(welcome, username);
		} else if (reply instanceof RegistrationStatus status && status.type() == RegistrationStatus.Type.PENDING) {
			System.out.println("Registration pending!");
		} else {
			System.out.println(reply);
		}
		return client;
	}

	public static ConcordClient loginWithToken(String host, int port) throws IOException {
		var client = new ConcordClient(host, port);
		var token = client.dataStore.getSessionToken(client.socket.getInetAddress().getHostName() + ":" + client.socket.getPort());
		if (token.isPresent()) {
			client.sendMessage(new ClientSessionResume(token.get()));
			Message reply = client.serializer.readMessage(client.in);
			if (reply instanceof ServerWelcome welcome) {
				client.initializeClientModel(welcome, "unknown");
			}
		} else {
			System.err.println("No session token!");
		}
		return client;
	}

	private void initializeClientModel(ServerWelcome welcomeData, String username) throws IOException {
		var model = new ClientModel(
				welcomeData.clientId(),
				username,
				welcomeData.currentChannelId(),
				welcomeData.currentChannelName(),
				welcomeData.metaData()
		);
		this.dataStore.saveSessionToken(this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort(), welcomeData.sessionToken());
		// Start fetching initial data for the channel we were initially put into.
		this.sendMessage(new ChatHistoryRequest(model.getCurrentChannelId(), ""));
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
