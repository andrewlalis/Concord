package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.gui2.*;
import lombok.Getter;
import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_client.event.ClientModelListener;
import nl.andrewl.concord_core.msg.types.ChannelUsersResponse;

import java.util.List;
import java.util.UUID;

/**
 * The main panel in which a user interacts with the application during normal
 * operation. In here, the user is shown a list of the most recent chats in
 * their current channel or thread, a text-box for sending a message, and some
 * meta information in the sidebars which provides the user with a list of all
 * threads and users in the server.
 */
public class ServerPanel extends Panel implements ClientModelListener {
	@Getter
	private final ChannelChatBox channelChatBox;
	private final ChannelList channelList;
	private final UserList userList;

	private final TextGUIThread guiThread;

	public ServerPanel(ConcordClient client, Window window) {
		super(new BorderLayout());
		this.guiThread = window.getTextGUI().getGUIThread();
		this.channelChatBox = new ChannelChatBox(client, window);
		this.channelList = new ChannelList(client);
		this.channelList.setChannels();
		this.userList = new UserList(client);

		Border b;
		b = Borders.doubleLine("Channels");
		b.setComponent(this.channelList);
		this.addComponent(b, BorderLayout.Location.LEFT);

		b = Borders.doubleLine("Users");
		b.setComponent(this.userList);
		this.addComponent(b, BorderLayout.Location.RIGHT);

		this.addComponent(this.channelChatBox, BorderLayout.Location.CENTER);
	}

	@Override
	public void channelMoved(UUID oldChannelId, UUID newChannelId) {
		this.getTextGUI().getGUIThread().invokeLater(() -> {
			this.channelList.setChannels();
			this.channelChatBox.getChatList().clearItems();
			this.channelChatBox.refreshBorder();
			this.channelChatBox.getInputTextBox().takeFocus();
		});
	}

	@Override
	public void usersUpdated(List<ChannelUsersResponse.UserData> users) {
		this.guiThread.invokeLater(() -> {
			this.userList.updateUsers(users);
		});
	}
}
