package nl.andrewl.concord_server;

import lombok.extern.java.Log;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.MoveToChannel;
import nl.andrewl.concord_server.event.ChannelMoveHandler;
import nl.andrewl.concord_server.event.ChatHandler;
import nl.andrewl.concord_server.event.MessageHandler;

import java.util.HashMap;
import java.util.Map;

@Log
public class EventManager {
	private final Map<Class<? extends Message>, MessageHandler<?>> messageHandlers;
	private final ConcordServer server;

	public EventManager(ConcordServer server) {
		this.server = server;
		this.messageHandlers = new HashMap<>();
		this.messageHandlers.put(Chat.class, new ChatHandler());
		this.messageHandlers.put(MoveToChannel.class, new ChannelMoveHandler());
	}

	@SuppressWarnings("unchecked")
	public <T extends Message> void handle(T message, ClientThread client) {
		MessageHandler<T> handler = (MessageHandler<T>) this.messageHandlers.get(message.getClass());
		if (handler != null) {
			try {
				handler.handle(message, client, this.server);
			} catch (Exception e) {
				log.warning("Exception occurred while handling message: " + e.getMessage());
			}
		}
	}
}
