package nl.andrewl.concord_client.event.handlers;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_client.event.MessageHandler;
import nl.andrewl.concord_core.msg.types.ChannelUsersResponse;

/**
 * When the client receives information about the list of known users, it will
 * update its model to show the new list.
 */
public class ChannelUsersResponseHandler implements MessageHandler<ChannelUsersResponse> {
	@Override
	public void handle(ChannelUsersResponse msg, ConcordClient client) throws Exception {
		client.getModel().setKnownUsers(msg.getUsers());
	}
}
