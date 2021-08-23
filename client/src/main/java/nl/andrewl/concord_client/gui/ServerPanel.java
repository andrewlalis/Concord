package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.gui2.*;
import lombok.Getter;
import nl.andrewl.concord_client.ClientMessageListener;
import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.*;

import java.io.IOException;

/**
 * The main panel in which a user interacts with the application during normal
 * operation. In here, the user is shown a list of the most recent chats in
 * their current channel or thread, a text-box for sending a message, and some
 * meta information in the sidebars which provides the user with a list of all
 * threads and users in the server.
 */
public class ServerPanel extends Panel implements ClientMessageListener {
	@Getter
	private final ChannelChatBox channelChatBox;
	private final ChannelList channelList;
	private final UserList userList;

	private final ConcordClient client;
	private final TextGUIThread guiThread;

	public ServerPanel(ConcordClient client, Window window) {
		super(new BorderLayout());
		this.guiThread = window.getTextGUI().getGUIThread();
		this.client = client;
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
	public void messageReceived(ConcordClient client, Message message) {
		if (message instanceof Chat chat) {
			this.channelChatBox.getChatList().addItem(chat);
		} else if (message instanceof MoveToChannel moveToChannel) {
			client.setCurrentChannelId(moveToChannel.getChannelId());
			try {
				client.sendMessage(new ChatHistoryRequest(moveToChannel.getChannelId(), ChatHistoryRequest.Source.CHANNEL, ""));
				client.sendMessage(new ChannelUsersRequest(moveToChannel.getChannelId()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.guiThread.invokeLater(() -> {
				this.channelList.setChannels();
				this.channelChatBox.getChatList().clearItems();
				this.channelChatBox.refreshBorder();
				this.channelChatBox.getInputTextBox().takeFocus();
			});
		} else if (message instanceof ChannelUsersResponse channelUsersResponse) {
			this.guiThread.invokeLater(() -> {
				this.userList.updateUsers(channelUsersResponse);
			});
		} else if (message instanceof ChatHistoryResponse chatHistoryResponse) {
			System.out.println("Got chat history response: " + chatHistoryResponse.getSourceId());
			System.out.println(chatHistoryResponse.getMessages());
			this.guiThread.invokeLater(() -> {
				this.channelChatBox.getChatList().clearItems();
				for (var chat : chatHistoryResponse.getMessages()) {
					this.channelChatBox.getChatList().addItem(chat);
				}
			});
		}
	}
}
