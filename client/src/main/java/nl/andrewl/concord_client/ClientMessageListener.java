package nl.andrewl.concord_client;

import nl.andrewl.concord_core.msg.Message;

import java.io.IOException;

public interface ClientMessageListener {
	void messageReceived(ConcordClient client, Message message) throws IOException;
}
