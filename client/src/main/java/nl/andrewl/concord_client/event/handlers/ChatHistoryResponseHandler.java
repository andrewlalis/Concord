package nl.andrewl.concord_client.event.handlers;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_client.event.MessageHandler;
import nl.andrewl.concord_core.msg.types.ChatHistoryResponse;

public class ChatHistoryResponseHandler implements MessageHandler<ChatHistoryResponse> {
	@Override
	public void handle(ChatHistoryResponse msg, ConcordClient client) {
		client.getModel().getChatHistory().setChats(msg.getMessages());
	}
}
