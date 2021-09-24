package nl.andrewl.concord_client.model;

import lombok.Getter;
import nl.andrewl.concord_client.event.ChatHistoryListener;
import nl.andrewl.concord_core.msg.types.chat.Chat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stores information about a snippet of chat history that the client is
 * currently viewing. This might be some older section of chats, or it could be
 * the currently-in-use channel chats.
 */
public class ChatHistory {
	@Getter
	private List<Chat> chats;

	private final List<ChatHistoryListener> chatHistoryListeners;

	public ChatHistory() {
		this.chats = new CopyOnWriteArrayList<>();
		this.chatHistoryListeners = new CopyOnWriteArrayList<>();
	}

	public void setChats(List<Chat> chats) {
		this.chats.clear();
		this.chats.addAll(chats);
		this.chatHistoryListeners.forEach(listener -> listener.chatUpdated(this));
	}

	public void addChat(Chat chat) {
		this.chats.add(chat);
		this.chatHistoryListeners.forEach(listener -> listener.chatAdded(chat));
	}

	public void addListener(ChatHistoryListener listener) {
		this.chatHistoryListeners.add(listener);
	}

	public void removeListener(ChatHistoryListener listener) {
		this.chatHistoryListeners.remove(listener);
	}
}
