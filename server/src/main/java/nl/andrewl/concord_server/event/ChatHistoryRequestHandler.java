package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.ChatHistoryRequest;
import nl.andrewl.concord_core.msg.types.ChatHistoryResponse;
import nl.andrewl.concord_core.msg.types.Error;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.channel.Channel;
import nl.andrewl.concord_server.client.ClientThread;
import org.dizitart.no2.*;
import org.dizitart.no2.filters.Filters;

import java.util.*;

/**
 * Handles client requests for sections of chat history for a particular channel.
 */
public class ChatHistoryRequestHandler implements MessageHandler<ChatHistoryRequest> {
	@Override
	public void handle(ChatHistoryRequest msg, ClientThread client, ConcordServer server) {
		// First try and find a public channel with the given id.
		var channel = server.getChannelManager().getChannelById(msg.getChannelId()).orElse(null);
		if (channel == null) {
			// Couldn't find a public channel, so look for a private channel this client is involved in.
			channel = server.getChannelManager().getPrivateChannel(client.getClientId(), msg.getChannelId()).orElse(null);
		}
		// If we couldn't find a public or private channel, give up.
		if (channel == null) {
			client.sendToClient(Error.warning("Unknown channel id."));
			return;
		}
		var params = msg.getQueryAsMap();
		if (params.containsKey("id")) {
			this.handleIdRequest(client, channel, params.get("id"));
		} else {
			Long count = this.getOrDefault(params, "count", (long) server.getConfig().getChatHistoryDefaultCount());
			if (count > server.getConfig().getChatHistoryMaxCount()) {
				return;
			}
			Long from = this.getOrDefault(params, "from", null);
			Long to = this.getOrDefault(params, "to", null);
			client.sendToClient(this.getResponse(channel, count, from, to));
		}
	}

	/**
	 * Handles a request for a single message from a channel.
	 * @param client The client who's requesting the data.
	 * @param channel The channel in which to search for the message.
	 * @param id The id of the message.
	 */
	private void handleIdRequest(ClientThread client, Channel channel, String id) {
		var cursor = channel.getMessageCollection().find(Filters.eq("id", id));
		List<Chat> chats = new ArrayList<>(1);
		for (var doc : cursor) {
			chats.add(this.read(doc));
		}
		client.sendToClient(new ChatHistoryResponse(channel.getId(), chats));
	}

	/**
	 * Gets a response for a standard chat history request, using a standard set
	 * of parameters.
	 * @param channel The channel to get chat history from.
	 * @param count The number of messages to retrieve.
	 * @param from If not null, only include messages made after this timestamp.
	 * @param to If not null, only include messages made before this timestamp.
	 * @return A chat history response.
	 */
	private ChatHistoryResponse getResponse(Channel channel, long count, Long from, Long to) {
		var col = channel.getMessageCollection();
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
			chats.add(this.read(doc));
		}
		Collections.reverse(chats);
		return new ChatHistoryResponse(channel.getId(), chats);
	}

	/**
	 * Helper method to read a {@link Chat} from a document retrieved from a
	 * collection.
	 * @param doc The document to read.
	 * @return The chat that was read.
	 */
	private Chat read(Document doc) {
		return new Chat(
				doc.get("id", UUID.class),
				doc.get("senderId", UUID.class),
				doc.get("senderNickname", String.class),
				doc.get("timestamp", Long.class),
				doc.get("message", String.class)
		);
	}

	/**
	 * Helper method to get a long value or fall back to a default.
	 * @param params The parameters to check.
	 * @param key The key to get the value for.
	 * @param defaultValue The default value to return if no value exists.
	 * @return The value that was found, or the default value.
	 */
	private Long getOrDefault(Map<String, String> params, String key, Long defaultValue) {
		String value = params.get(key);
		if (value == null) return defaultValue;
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}
}
