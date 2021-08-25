package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.ChatHistoryRequest;
import nl.andrewl.concord_core.msg.types.ChatHistoryResponse;
import nl.andrewl.concord_server.Channel;
import nl.andrewl.concord_server.ClientThread;
import nl.andrewl.concord_server.ConcordServer;
import org.dizitart.no2.*;
import org.dizitart.no2.filters.Filters;

import java.util.*;

/**
 * Handles client requests for sections of chat history for a particular channel.
 */
public class ChatHistoryRequestHandler implements MessageHandler<ChatHistoryRequest> {
	@Override
	public void handle(ChatHistoryRequest msg, ClientThread client, ConcordServer server) {
		var optionalChannel = server.getChannelManager().getChannelById(msg.getChannelId());
		if (optionalChannel.isPresent()) {
			var channel = optionalChannel.get();
			var params = msg.getQueryAsMap();
			Long count = this.getOrDefault(params, "count", (long) server.getConfig().getChatHistoryDefaultCount());
			if (count > server.getConfig().getChatHistoryMaxCount()) {
				return;
			}
			Long from = this.getOrDefault(params, "from", null);
			Long to = this.getOrDefault(params, "to", null);
			client.sendToClient(this.getResponse(channel, count, from, to));
		}
	}

	private Long getOrDefault(Map<String, String> params, String key, Long defaultValue) {
		String value = params.get(key);
		if (value == null) return defaultValue;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private ChatHistoryResponse getResponse(Channel channel, long count, Long from, Long to) {
		var col = channel.getServer().getDb().getCollection("channel-" + channel.getId());
		Cursor cursor;
		FindOptions options = FindOptions.sort("timestamp", SortOrder.Descending).thenLimit(0, (int) count);
		List<Filter> filters = new ArrayList<>(2);
		if (from != null) {
			filters.add(Filters.gt("timestamp", from));
		}
		if (to != null) {
			filters.add(Filters.lt("timestamp", to));
		}
		if (filters.isEmpty()) {
			cursor = col.find(options);
		} else {
			cursor = col.find(Filters.and(filters.toArray(new Filter[0])), options);
		}

		List<Chat> chats = new ArrayList<>((int) count);
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
		return new ChatHistoryResponse(channel.getId(), chats);
	}
}
