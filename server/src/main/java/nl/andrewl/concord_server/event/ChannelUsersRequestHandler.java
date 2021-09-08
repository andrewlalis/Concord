package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.ChannelUsersRequest;
import nl.andrewl.concord_core.msg.types.ChannelUsersResponse;
import nl.andrewl.concord_server.client.ClientThread;
import nl.andrewl.concord_server.ConcordServer;

public class ChannelUsersRequestHandler implements MessageHandler<ChannelUsersRequest> {
	@Override
	public void handle(ChannelUsersRequest msg, ClientThread client, ConcordServer server) throws Exception {
		var optionalChannel = server.getChannelManager().getChannelById(msg.getChannelId());
		if (optionalChannel.isPresent()) {
			var channel = optionalChannel.get();
			client.sendToClient(new ChannelUsersResponse(channel.getUserData()));
		}
	}
}
