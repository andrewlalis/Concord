package nl.andrewl.concord_client.event.handlers;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_client.event.MessageHandler;
import nl.andrewl.concord_core.msg.types.ChannelUsersRequest;
import nl.andrewl.concord_core.msg.types.ChatHistoryRequest;
import nl.andrewl.concord_core.msg.types.MoveToChannel;

public class ChannelMovedHandler implements MessageHandler<MoveToChannel> {
	@Override
	public void handle(MoveToChannel msg, ConcordClient client) throws Exception {
		client.getModel().setCurrentChannelId(msg.getChannelId());
		client.sendMessage(new ChatHistoryRequest(msg.getChannelId(), ""));
		client.sendMessage(new ChannelUsersRequest(msg.getChannelId()));
	}
}
