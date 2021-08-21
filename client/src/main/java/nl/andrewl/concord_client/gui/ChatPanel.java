package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import nl.andrewl.concord_client.ClientMessageListener;
import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_core.msg.Message;
import nl.andrewl.concord_core.msg.types.Chat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The main panel in which a user interacts with the application during normal
 * operation. In here, the user is shown a list of the most recent chats in
 * their current channel or thread, a text-box for sending a message, and some
 * meta information in the sidebars which provides the user with a list of all
 * threads and users in the server.
 */
public class ChatPanel extends Panel implements ClientMessageListener {
	private final ChatList chatList;
	private final TextBox inputTextBox;
	private final ChannelList channelList;
	private final UserList userList;

	private final ConcordClient client;

	public ChatPanel(ConcordClient client, Window window) {
		super(new BorderLayout());
		this.client = client;
		this.chatList = new ChatList();
		this.inputTextBox = new TextBox("", TextBox.Style.MULTI_LINE);
		this.inputTextBox.setCaretWarp(true);
		this.inputTextBox.setPreferredSize(new TerminalSize(0, 3));

		this.channelList = new ChannelList();
		this.channelList.addItem("general");
		this.channelList.addItem("memes");
		this.channelList.addItem("testing");
		this.userList = new UserList();
		this.userList.addItem("andrew");
		this.userList.addItem("tester");

		window.addWindowListener(new WindowListenerAdapter() {
			@Override
			public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
				if (keyStroke.getKeyType() == KeyType.Enter) {
					if (keyStroke.isShiftDown()) {
						System.out.println("Adding newline");
					} else {
						String text = inputTextBox.getText();
						if (text != null && !text.isBlank()) {
							try {
								System.out.println("Sending: " + text.trim());
								client.sendChat(text.trim());
								inputTextBox.setText("");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						deliverEvent.set(false);
					}
				}
			}
		});
		Border b;
		b = Borders.doubleLine("Channels");
		b.setComponent(this.channelList);
		this.addComponent(b, BorderLayout.Location.LEFT);

		b = Borders.doubleLine("Users");
		b.setComponent(this.userList);
		this.addComponent(b, BorderLayout.Location.RIGHT);

		b = Borders.doubleLine("#general");
		b.setComponent(this.chatList);
		this.addComponent(b, BorderLayout.Location.CENTER);

		this.addComponent(this.inputTextBox, BorderLayout.Location.BOTTOM);
		this.inputTextBox.takeFocus();
	}

	@Override
	public void messageReceived(ConcordClient client, Message message) throws IOException {
		if (message instanceof Chat chat) {
			this.chatList.addItem(chat);
		}
	}
}
