package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_server.ClientThread;
import nl.andrewl.concord_server.ConcordServer;
import org.dizitart.no2.Document;

import java.io.IOException;
import java.util.Map;

public class ChatHandler implements MessageHandler<Chat> {
	@Override
	public void handle(Chat msg, ClientThread client, ConcordServer server) throws IOException {
		server.getExecutorService().submit(() -> {
			var collection = client.getCurrentChannel().getMessageCollection();
			Document doc = new Document(Map.of(
					"senderId", msg.getSenderId(),
					"senderNickname", msg.getSenderNickname(),
					"timestamp", msg.getTimestamp(),
					"message", msg.getMessage()
			));
			collection.insert(doc);
		});
		System.out.printf("#%s | %s: %s\n", client.getCurrentChannel(), client.getClientNickname(), msg.getMessage());
		client.getCurrentChannel().sendMessage(msg);
	}
}
