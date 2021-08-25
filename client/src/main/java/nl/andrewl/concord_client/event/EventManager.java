package nl.andrewl.concord_client.event;

import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_core.msg.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
	private final Map<Class<? extends Message>, List<MessageHandler<?>>> messageHandlers;
	private final ConcordClient client;

	public EventManager(ConcordClient client) {
		this.client = client;
		this.messageHandlers = new ConcurrentHashMap<>();
	}

	public <T extends Message> void addHandler(Class<T> messageClass, MessageHandler<T> handler) {
		var handlers = this.messageHandlers.computeIfAbsent(messageClass, k -> new CopyOnWriteArrayList<>());
		handlers.add(handler);
	}

	@SuppressWarnings("unchecked")
	public <T extends Message> void handle(T message) {
		var handlers = this.messageHandlers.get(message.getClass());
		if (handlers != null) {
			for (var handler : handlers) {
				try {
					((MessageHandler<T>) handler).handle(message, this.client);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
