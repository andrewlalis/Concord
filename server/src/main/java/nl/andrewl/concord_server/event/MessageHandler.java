package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_server.client.ClientThread;
import nl.andrewl.concord_server.ConcordServer;

/**
 * Defines a component which can handle messages of a certain type which were
 * received from a client.
 * @param <T> The type of message to be handled.
 */
public interface MessageHandler<T extends Message> {
	void handle(T msg, ClientThread client, ConcordServer server) throws Exception;
}
