package nl.andrewl.concord_client.event;

import nl.andrewl.concord_client.model.ChatHistory;
import nl.andrewl.concord_core.msg.types.Chat;

public interface ChatHistoryListener {
	default void chatAdded(Chat chat) {}

	default void chatRemoved(Chat chat) {}

	default void chatUpdated(ChatHistory history) {}
}
