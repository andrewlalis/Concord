package nl.andrewl.concord_client.event.handlers;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_client.event.MessageHandler;
import nl.andrewl.concord_core.msg.types.chat.ChatHistoryResponse;

import java.util.Arrays;
import java.util.List;

public class ChatHistoryResponseHandler implements MessageHandler<ChatHistoryResponse> {
	@Override
	public void handle(ChatHistoryResponse msg, ConcordClient client) {
		client.getModel().getChatHistory().setChats(Arrays.asList(msg.messages()));
	}
}
