package nl.andrewl.concord_server;

import lombok.extern.java.Log;
import nl.andrewl.concord_core.msg.Serializer;
import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.ChatHistoryRequest;
import org.dizitart.no2.Document;
import org.dizitart.no2.Nitrite;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Log
public class ConcordServer implements Runnable {
	private final Map<Long, ClientThread> clients = new ConcurrentHashMap<>(32);
	private final int port;
	private final Random random;
	private final Nitrite db;

	public ConcordServer(int port) {
		this.port = port;
		this.random = new SecureRandom();
		this.db = Nitrite.builder()
				.filePath("concord-server.db")
				.openOrCreate();
	}

	public long registerClient(ClientThread clientThread) {
		long id = this.random.nextLong();
		log.info("Registering new client " + clientThread.getClientNickname() + " with id " + id);
		this.clients.put(id, clientThread);
		return id;
	}

	public void deregisterClient(long clientId) {
		this.clients.remove(clientId);
	}

	public void handleChat(Chat chat) {
		var collection = db.getCollection("channel-TEST");
		long messageId = this.random.nextLong();
		Document doc = Document.createDocument(Long.toHexString(messageId), "message")
				.put("senderId", Long.toHexString(chat.getSenderId()))
				.put("senderNickname", chat.getSenderNickname())
				.put("timestamp", chat.getTimestamp())
				.put("message", chat.getMessage());
		collection.insert(doc);
		db.commit();
		System.out.println(chat.getSenderNickname() + ": " + chat.getMessage());
		ByteArrayOutputStream baos = new ByteArrayOutputStream(chat.getByteCount());
		try {
			Serializer.writeMessage(chat, new DataOutputStream(baos));
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		byte[] data = baos.toByteArray();
		for (var client : clients.values()) {
			client.sendToClient(data);
		}
	}

	public void handleHistoryRequest(ChatHistoryRequest request, ClientThread clientThread) {

	}

	@Override
	public void run() {
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(this.port);
			log.info("Opened server on port " + this.port);
			while (true) {
				Socket socket = serverSocket.accept();
				log.info("Accepted new socket connection.");
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
