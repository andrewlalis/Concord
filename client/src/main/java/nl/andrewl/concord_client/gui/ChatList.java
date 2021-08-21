package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.gui2.AbstractListBox;
import nl.andrewl.concord_core.msg.types.Chat;

public class ChatList extends AbstractListBox<Chat, ChatList> {
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
}
