package nl.andrewl.concord_server;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.Identification;
import nl.andrewl.concord_core.msg.types.ServerWelcome;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

/**
 * This thread is responsible for handling the connection to a single client of
 * a server.
 */
@Log
public class ClientThread extends Thread {
	private final Socket socket;
	private final DataInputStream in;
	private final DataOutputStream out;

	private final ConcordServer server;

	private UUID clientId = null;
	@Getter
	private String clientNickname = null;

	@Getter
	@Setter
	private Channel currentChannel;

	private volatile boolean running;

	public ClientThread(Socket socket, ConcordServer server) throws IOException {
		this.socket = socket;
		this.server = server;
		this.in = new DataInputStream(socket.getInputStream());
		this.out = new DataOutputStream(socket.getOutputStream());
	}

	public void sendToClient(Message message) {
		try {
			Serializer.writeMessage(message, this.out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendToClient(byte[] bytes) {
		try {
			this.out.write(bytes);
			this.out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
			log.warning("Could not identify the client; aborting connection.");
			this.running = false;
		}

		while (this.running) {
			try {
				var msg = Serializer.readMessage(this.in);
				this.server.getEventManager().handle(msg, this);
			} catch (IOException e) {
				log.info("Client disconnected: " + e.getMessage());
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
				var msg = Serializer.readMessage(this.in);
				if (msg instanceof Identification id) {
					this.clientNickname = id.getNickname();
					this.clientId = this.server.registerClient(id, this);
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			attempts++;
		}
		return false;
	}
}
