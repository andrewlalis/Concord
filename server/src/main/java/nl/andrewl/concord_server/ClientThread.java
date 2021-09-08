package nl.andrewl.concord_server;

import lombok.Getter;
import lombok.Setter;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.Identification;
import nl.andrewl.concord_core.msg.types.UserData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

/**
 * This thread is responsible for handling the connection to a single client of
 * a server. The client thread acts as the server's representation of a client.
 */
public class ClientThread extends Thread {
	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;

	private final ConcordServer server;

	@Getter
	@Setter
	private UUID clientId = null;
	@Getter
	@Setter
	private String clientNickname = null;

	@Getter
	@Setter
	private Channel currentChannel;

	private volatile boolean running;

	/**
	 * Constructs a new client thread.
	 * @param socket The socket to use to communicate with the client.
	 * @param server The server to which this thread belongs.
	 * @throws IOException If we cannot obtain the input and output streams from
	 * the socket.
	 */
	public ClientThread(Socket socket, ConcordServer server) throws IOException {
		this.socket = socket;
		this.server = server;
		this.in = new DataInputStream(socket.getInputStream());
		this.out = new DataOutputStream(socket.getOutputStream());
	}

	/**
	 * Sends the given message to the client. Note that this method is
	 * synchronized, such that multiple messages cannot be sent simultaneously.
	 * @param message The message to send.
	 */
	public synchronized void sendToClient(Message message) {
		try {
			this.server.getSerializer().writeMessage(message, this.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends the given bytes to the client. This is a shortcut for {@link ClientThread#sendToClient(Message)}
	 * which can be used to optimize message sending in certain instances.
	 * @param bytes The bytes to send.
	 */
	public synchronized void sendToClient(byte[] bytes) {
		try {
			this.out.write(bytes);
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Shuts down this client thread, closing the underlying socket and setting
	 * {@link ClientThread#running} to false so that the main thread loop will
	 * exit shortly.
	 */
	public void shutdown() {
		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.running = false;
	}

	@Override
	public void run() {
		this.running = true;
		if (!identifyClient()) {
			System.err.println("Could not identify the client; aborting connection.");
			this.running = false;
		}

		while (this.running) {
			try {
				var msg = this.server.getSerializer().readMessage(this.in);
				this.server.getEventManager().handle(msg, this);
			} catch (IOException e) {
				this.running = false;
			}
		}

		if (this.clientId != null) {
			this.server.deregisterClient(this.clientId);
		}
		try {
			if (!this.socket.isClosed()) {
				this.socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Initial method that attempts to obtain identification information from a
	 * newly-connected client. It is the intent that we should close the socket
	 * if the client is not able to identify itself.
	 * @return True if we were able to obtain identification from the client, or
	 * false otherwise.
	 */
	private boolean identifyClient() {
		int attempts = 0;
		while (attempts < 5) {
			try {
				var msg = this.server.getSerializer().readMessage(this.in);
				if (msg instanceof Identification id) {
					this.server.registerClient(id, this);
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			attempts++;
		}
		return false;
	}

	public UserData toData() {
		return new UserData(this.clientId, this.clientNickname);
	}

	@Override
	public String toString() {
		return this.clientNickname + " (" + this.clientId + ")";
	}
}
