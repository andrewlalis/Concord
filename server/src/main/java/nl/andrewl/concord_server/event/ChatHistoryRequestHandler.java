package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.ChatHistoryRequest;
import nl.andrewl.concord_core.msg.types.ChatHistoryResponse;
import nl.andrewl.concord_server.ClientThread;
import nl.andrewl.concord_server.ConcordServer;
import org.dizitart.no2.Document;
import org.dizitart.no2.FindOptions;
import org.dizitart.no2.SortOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ChatHistoryRequestHandler implements MessageHandler<ChatHistoryRequest> {
	@Override
	public void handle(ChatHistoryRequest msg, ClientThread client, ConcordServer server) {
		var optionalChannel = server.getChannelManager().getChannelById(msg.getSourceId());
		if (optionalChannel.isPresent()) {
			var channel = optionalChannel.get();
			System.out.println("Looking for chats in channel-" + channel.getId());
			var col = server.getDb().getCollection("channel-" + channel.getId());
			var cursor = col.find(
					FindOptions.sort("timestamp", SortOrder.Descending)
						.thenLimit(0, 10)
			);
			List<Chat> chats = new ArrayList<>(10);
			for (Document doc : cursor) {
				chats.add(new Chat(
						doc.get("senderId", UUID.class),
						doc.get("senderNickname", String.class),
						doc.get("timestamp", Long.class),
						doc.get("message", String.class)
				));
			}
			col.close();
			chats.sort(Comparator.comparingLong(Chat::getTimestamp));
			client.sendToClient(new ChatHistoryResponse(msg.getSourceId(), msg.getSourceType(), chats));
		}
	}
}
