package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import lombok.Getter;
import nl.andrewl.concord_client.ConcordClient;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This panel occupies the center of the interface, and displays the list of
 * recent messages, along with an input text box for the user to type messages
 * into.
 */
public class ChannelChatBox extends Panel {
	private final ConcordClient client;
	private Border chatBorder;
	@Getter
	private final ChatList chatList;
	@Getter
	private final TextBox inputTextBox;
	public ChannelChatBox(ConcordClient client, Window window) {
		super(new BorderLayout());
		this.client = client;
		this.chatList = new ChatList();
		this.client.getModel().getChatHistory().addListener(this.chatList);
		this.inputTextBox = new TextBox("", TextBox.Style.MULTI_LINE);
		this.inputTextBox.setCaretWarp(true);
		this.inputTextBox.setPreferredSize(new TerminalSize(0, 3));

		window.addWindowListener(new WindowListenerAdapter() {
			@Override
			public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean deliverEvent) {
				if (keyStroke.getKeyType() == KeyType.Enter && inputTextBox.isFocused() && !keyStroke.isShiftDown()) {
					String text = inputTextBox.getText();
					if (text != null && !text.isBlank()) {
						try {
							client.sendChat(text.trim());
							inputTextBox.setText("");
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					deliverEvent.set(false);
				}
			}
		});
		this.refreshBorder();
		this.addComponent(this.inputTextBox, BorderLayout.Location.BOTTOM);
	}

	public void refreshBorder() {
		String name = client.getModel().getServerMetaData().getChannels().stream()
				.filter(channelData -> channelData.getId().equals(client.getModel().getCurrentChannelId()))
				.findAny().orElseThrow().getName();
		if (this.chatBorder != null) this.removeComponent(this.chatBorder);
		this.chatBorder = Borders.doubleLine("#" + name);
		this.chatBorder.setComponent(this.chatList);
		this.addComponent(this.chatBorder, BorderLayout.Location.CENTER);
	}
}
