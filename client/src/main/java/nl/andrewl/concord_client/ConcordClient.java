package nl.andrewl.concord_client;

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
import nl.andrewl.concord_client.event.handlers.ChannelUsersResponseHandler;
import nl.andrewl.concord_client.event.handlers.ChatHistoryResponseHandler;
import nl.andrewl.concord_client.event.handlers.ServerMetaDataHandler;
import nl.andrewl.concord_client.gui.MainWindow;
import nl.andrewl.concord_client.model.ClientModel;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ConcordClient implements Runnable {
	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;

	@Getter
	private final ClientModel model;

	private final EventManager eventManager;

	private volatile boolean running;

	public ConcordClient(String host, int port, String nickname) throws IOException {
		this.eventManager = new EventManager(this);
		this.socket = new Socket(host, port);
		this.in = new DataInputStream(this.socket.getInputStream());
		this.out = new DataOutputStream(this.socket.getOutputStream());
		Serializer.writeMessage(new Identification(nickname), this.out);
		Message reply = Serializer.readMessage(this.in);
		if (reply instanceof ServerWelcome welcome) {
			this.model = new ClientModel(welcome.getClientId(), nickname, welcome.getCurrentChannelId(), welcome.getMetaData());
			// Start fetching initial data for the channel we were initially put into.
			this.sendMessage(new ChannelUsersRequest(this.model.getCurrentChannelId()));
			this.sendMessage(new ChatHistoryRequest(this.model.getCurrentChannelId(), ""));
		} else {
			throw new IOException("Unexpected response from the server after sending identification message.");
		}

		// Add event listeners.
		this.eventManager.addHandler(MoveToChannel.class, new ChannelMovedHandler());
		this.eventManager.addHandler(ChannelUsersResponse.class, new ChannelUsersResponseHandler());
		this.eventManager.addHandler(ChatHistoryResponse.class, new ChatHistoryResponseHandler());
		this.eventManager.addHandler(Chat.class, (msg, client) -> client.getModel().getChatHistory().addChat(msg));
		this.eventManager.addHandler(ServerMetaData.class, new ServerMetaDataHandler());
	}

	public void sendMessage(Message message) throws IOException {
		Serializer.writeMessage(message, this.out);
	}

	public void sendChat(String message) throws IOException {
		Serializer.writeMessage(new Chat(this.model.getId(), this.model.getNickname(), System.currentTimeMillis(), message), this.out);
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
				Message msg = Serializer.readMessage(this.in);
				this.eventManager.handle(msg);
			} catch (IOException e) {
				e.printStackTrace();
				this.running = false;
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
