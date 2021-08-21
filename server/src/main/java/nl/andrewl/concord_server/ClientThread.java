package nl.andrewl.concord_server;

import lombok.Getter;
import lombok.extern.java.Log;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.Identification;
import nl.andrewl.concord_core.msg.types.ServerWelcome;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

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

	private Long clientId = null;
	@Getter
	private String clientNickname = null;

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

	@Override
	public void run() {
		if (!identifyClient()) {
			log.warning("Could not identify the client; aborting connection.");
			return;
		}

		while (true) {
			try {
				var msg = Serializer.readMessage(this.in);
				if (msg instanceof Chat chat) {
					this.server.handleChat(chat);
				}
			} catch (IOException e) {
				log.info("Client disconnected: " + e.getMessage());
				if (this.clientId != null) {
					this.server.deregisterClient(this.clientId);
				}
				break;
			}
		}
	}

	private boolean identifyClient() {
		int attempts = 0;
		while (attempts < 5) {
			try {
				var msg = Serializer.readMessage(this.in);
				if (msg instanceof Identification id) {
					this.clientId = this.server.registerClient(this);
					this.clientNickname = id.getNickname();
					var reply = new ServerWelcome();
					reply.setClientId(this.clientId);
					Serializer.writeMessage(reply, this.out);
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
