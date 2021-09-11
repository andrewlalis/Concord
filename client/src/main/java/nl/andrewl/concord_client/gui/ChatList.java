package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.gui2.AbstractListBox;
import nl.andrewl.concord_client.event.ChatHistoryListener;
import nl.andrewl.concord_client.model.ChatHistory;
import nl.andrewl.concord_core.msg.types.Chat;

/**
 * This chat list shows a section of chat messages that have been sent in a
 * single channel (server channel, thread, or direct message).
 */
public class ChatList extends AbstractListBox<Chat, ChatList> implements ChatHistoryListener {
	/**
	 * Adds one more item to the list box, at the end.
	 *
	 * @param item Item to add to the list box
	 * @return Itself
	 */
	@Override
	public synchronized ChatList addItem(Chat item) {
		super.addItem(item);
		this.setSelectedIndex(this.getItemCount() - 1);
		return this;
	}

	/**
	 * Method that constructs the {@code ListItemRenderer} that this list box should use to draw the elements of the
	 * list box. This can be overridden to supply a custom renderer. Note that this is not the renderer used for the
	 * entire list box but for each item, called one by one.
	 *
	 * @return {@code ListItemRenderer} to use when drawing the items in the list
	 */
	@Override
	protected ListItemRenderer<Chat, ChatList> createDefaultListItemRenderer() {
		return new ChatRenderer();
	}

	@Override
	public void chatAdded(Chat chat) {
		this.getTextGUI().getGUIThread().invokeLater(() -> {
			this.addItem(chat);
		});
	}

	@Override
	public void chatRemoved(Chat chat) {
		for (int i = 0; i < this.getItemCount(); i++) {
			if (this.getItemAt(i).equals(chat)) {
				this.removeItem(i);
				return;
			}
		}
	}

	@Override
	public void chatUpdated(ChatHistory history) {
		this.getTextGUI().getGUIThread().invokeLater(() -> {
			this.clearItems();
			System.out.println("Cleared chats");
			for (var chat : history.getChats()) {
				System.out.println("Adding chat: " + chat);
				this.addItem(chat);
			}
		});
	}
}
