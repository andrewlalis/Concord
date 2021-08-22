package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_server.ClientThread;
import nl.andrewl.concord_server.ConcordServer;

public interface MessageHandler<T extends Message> {
	void handle(T msg, ClientThread client, ConcordServer server) throws Exception;
}
