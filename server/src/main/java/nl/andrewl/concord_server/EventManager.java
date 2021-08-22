package nl.andrewl.concord_server;

import lombok.extern.java.Log;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.ChannelUsersRequest;
import nl.andrewl.concord_core.msg.types.Chat;
import nl.andrewl.concord_core.msg.types.ChatHistoryRequest;
import nl.andrewl.concord_core.msg.types.MoveToChannel;
import nl.andrewl.concord_server.event.*;

import java.util.HashMap;
import java.util.Map;

/**
 * The event manager is responsible for the server's ability to respond to
 * various client requests.
 */
@Log
public class EventManager {
	private final Map<Class<? extends Message>, MessageHandler<?>> messageHandlers;
	private final ConcordServer server;

	public EventManager(ConcordServer server) {
		this.server = server;
		this.messageHandlers = new HashMap<>();
		this.messageHandlers.put(Chat.class, new ChatHandler());
		this.messageHandlers.put(MoveToChannel.class, new ChannelMoveHandler());
		this.messageHandlers.put(ChannelUsersRequest.class, new ChannelUsersRequestHandler());
		this.messageHandlers.put(ChatHistoryRequest.class, new ChatHistoryRequestHandler());
	}

	/**
	 * Handles a new message that was sent from a client. Tries to find an
	 * appropriate handler for the message, and if one is found, calls the
	 * {@link MessageHandler#handle(Message, ClientThread, ConcordServer)}
	 * method on it.
	 * <p>
	 *     Note that it is expected that client threads will invoke this method
	 *     during their {@link ClientThread#run()} method, so concurrent
	 *     invocation is expected.
	 * </p>
	 * @param message The message that was sent by a client.
	 * @param client The client thread that is used for communicating with the
	 *               client.
	 * @param <T> The type of message.
	 */
	@SuppressWarnings("unchecked")
	public <T extends Message> void handle(T message, ClientThread client) {
		MessageHandler<T> handler = (MessageHandler<T>) this.messageHandlers.get(message.getClass());
		if (handler != null) {
			try {
				handler.handle(message, client, this.server);
			} catch (Exception e) {
				e.printStackTrace();
				log.warning("Exception occurred while handling message: " + e.getMessage());
			}
		}
	}
}
