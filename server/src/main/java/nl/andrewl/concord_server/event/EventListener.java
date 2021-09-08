package nl.andrewl.concord_server.event;

import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_server.client.ClientThread;
import nl.andrewl.concord_server.ConcordServer;

public interface EventListener {
	default void chatMessageReceived(ConcordServer server, Chat chat, ClientThread client) {}
}
