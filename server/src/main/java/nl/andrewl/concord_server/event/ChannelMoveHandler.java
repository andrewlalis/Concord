package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.Error;
import nl.andrewl.concord_core.msg.types.MoveToChannel;
import nl.andrewl.concord_server.ConcordServer;
import nl.andrewl.concord_server.client.ClientThread;

import java.util.List;
import java.util.Set;

/**
 * Handles client requests to move to another channel. We first check if the id
 * which the client sent refers to a channel, in which case we move them to that
 * channel. Otherwise, we look for a client with that id, and try to move the
 * requester into a private channel with them.
 */
public class ChannelMoveHandler implements MessageHandler<MoveToChannel> {
	@Override
	public void handle(MoveToChannel msg, ClientThread client, ConcordServer server) {
		var optionalChannel = server.getChannelManager().getChannelById(msg.getId());
		if (optionalChannel.isPresent()) {
			server.getChannelManager().moveToChannel(client, optionalChannel.get());
		} else {
			var optionalClient = server.getClientManager().getClientById(msg.getId());
			if (optionalClient.isPresent()) {
				var privateChannel = server.getChannelManager().getPrivateChannel(Set.of(
						client.getClientId(),
						optionalClient.get().getClientId()
				));
				server.getChannelManager().moveToChannel(client, privateChannel);
			} else {
				client.sendToClient(Error.warning("Unknown channel or client id."));
			}
		}
	}
}
