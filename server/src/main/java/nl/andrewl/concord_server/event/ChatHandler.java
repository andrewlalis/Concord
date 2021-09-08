package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.Error;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.client.ClientThread;
import org.dizitart.no2.Document;

import java.io.IOException;
import java.util.Map;

/**
 * This handler is responsible for taking incoming chat messages and saving them
 * to the channel's message collection, and then relaying the new message to all
 * clients in the channel.
 */
public class ChatHandler implements MessageHandler<Chat> {
	@Override
	public void handle(Chat msg, ClientThread client, ConcordServer server) throws IOException {
		if (msg.getMessage().length() > server.getConfig().getMaxMessageLength()) {
			client.getCurrentChannel().sendMessage(Error.warning("Message is too long."));
			return;
		}
		/*
		When we receive a message from the client, it will have a random UUID.
		A compromised client could try and send a duplicate or otherwise
		malicious UUID, so we overwrite it with a server-generated id which we
		know is safe.
		 */
		msg.setId(server.getIdProvider().newId());
		var collection = client.getCurrentChannel().getMessageCollection();
		Document doc = new Document(Map.of(
				"id", msg.getId(),
				"senderId", msg.getSenderId(),
				"senderNickname", msg.getSenderNickname(),
				"timestamp", msg.getTimestamp(),
				"message", msg.getMessage()
		));
		collection.insert(doc);
		System.out.printf("#%s | %s: %s\n", client.getCurrentChannel(), client.getClientNickname(), msg.getMessage());
		client.getCurrentChannel().sendMessage(msg);
	}
}
