package nl.andrewl.concord_client.event.handlers;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_client.event.MessageHandler;
import nl.andrewl.concord_core.msg.types.channel.MoveToChannel;
import nl.andrewl.concord_core.msg.types.chat.ChatHistoryRequest;

/**
 * When the client receives a {@link MoveToChannel} message, it means that the
 * server has told the client that it has been moved to the indicated channel.
 * Thus, the client must now update its model and request the relevant info from
 * the server about the new channel it's in.
 */
public class ChannelMovedHandler implements MessageHandler<MoveToChannel> {
	@Override
	public void handle(MoveToChannel msg, ConcordClient client) throws Exception {
		client.getModel().setCurrentChannel(msg.id(), msg.channelName());
		client.sendMessage(new ChatHistoryRequest(msg.id()));
	}
}
