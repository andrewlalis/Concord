package nl.andrewl.concord_client.event;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_core.msg.Message;

public interface MessageHandler<T extends Message> {
	void handle(T msg, ConcordClient client) throws Exception;
}
