package nl.andrewl.concord_client.event.handlers;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_client.event.MessageHandler;
import nl.andrewl.concord_core.msg.types.ServerMetaData;

public class ServerMetaDataHandler implements MessageHandler<ServerMetaData> {
	@Override
	public void handle(ServerMetaData msg, ConcordClient client) {
		client.getModel().setServerMetaData(msg);
	}
}
