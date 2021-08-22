package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.MoveToChannel;
import nl.andrewl.concord_server.ClientThread;
import nl.andrewl.concord_server.ConcordServer;

/**
 * Handles client requests to move to another channel.
 */
public class ChannelMoveHandler implements MessageHandler<MoveToChannel> {
	@Override
	public void handle(MoveToChannel msg, ClientThread client, ConcordServer server) {
		var optionalChannel = server.getChannelManager().getChannelById(msg.getChannelId());
		optionalChannel.ifPresent(channel -> server.getChannelManager().moveToChannel(client, channel));
	}
}
