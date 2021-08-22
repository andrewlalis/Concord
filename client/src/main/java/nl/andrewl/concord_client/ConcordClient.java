package nl.andrewl.concord_client;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import lombok.Getter;
import lombok.Setter;
import nl.andrewl.concord_client.gui.MainWindow;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.MessageUtils;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.Identification;
import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_core.msg.types.ServerWelcome;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ConcordClient implements Runnable {

	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;
	private final UUID id;
	private final String nickname;
	@Getter
	@Setter
	private UUID currentChannelId;
	@Getter
	private ServerMetaData serverMetaData;
	private final Set<ClientMessageListener> messageListeners;
	private volatile boolean running;

	public ConcordClient(String host, int port, String nickname) throws IOException {
		this.socket = new Socket(host, port);
		this.in = new DataInputStream(this.socket.getInputStream());
		this.out = new DataOutputStream(this.socket.getOutputStream());
		this.nickname = nickname;
		Serializer.writeMessage(new Identification(nickname), this.out);
		Message reply = Serializer.readMessage(this.in);
		if (reply instanceof ServerWelcome welcome) {
			this.id = welcome.getClientId();
			this.currentChannelId = welcome.getCurrentChannelId();
			this.serverMetaData = welcome.getMetaData();
		} else {
			throw new IOException("Unexpected response from the server after sending identification message.");
		}
		this.messageListeners = new HashSet<>();
	}

	public void addListener(ClientMessageListener listener) {
		this.messageListeners.add(listener);
	}

	public void removeListener(ClientMessageListener listener) {
		this.messageListeners.remove(listener);
	}

	public void sendMessage(Message message) throws IOException {
		Serializer.writeMessage(message, this.out);
	}

	public void sendChat(String message) throws IOException {
		Serializer.writeMessage(new Chat(this.id, this.nickname, System.currentTimeMillis(), message), this.out);
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
				for (var listener : this.messageListeners) {
					listener.messageReceived(this, msg);
				}
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
