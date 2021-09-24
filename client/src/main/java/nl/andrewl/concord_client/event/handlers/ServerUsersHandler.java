package nl.andrewl.concord_client.event.handlers;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_client.event.MessageHandler;
import nl.andrewl.concord_core.msg.types.ServerUsers;

import java.util.Arrays;

public class ServerUsersHandler implements MessageHandler<ServerUsers> {
	@Override
	public void handle(ServerUsers msg, ConcordClient client) {
		client.getModel().setKnownUsers(Arrays.asList(msg.users()));
	}
}
