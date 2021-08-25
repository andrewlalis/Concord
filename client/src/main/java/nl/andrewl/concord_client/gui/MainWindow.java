package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.dialogs.TextInputDialogBuilder;
import com.googlecode.lanterna.input.KeyStroke;
import nl.andrewl.concord_client.ConcordClient;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainWindow extends BasicWindow {
	public MainWindow() {
		super("Concord");
		Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

		Button button = new Button("Connect to Server");
		button.addListener(b -> connectToServer());
		panel.addComponent(button);

		this.setComponent(panel);
		this.addWindowListener(new WindowListenerAdapter() {
			@Override
			public void onInput(Window basePane, KeyStroke keyStroke, AtomicBoolean hasBeenHandled) {
				if (keyStroke.getCharacter() != null && keyStroke.getCharacter() == 'c' && keyStroke.isCtrlDown()) {
					System.exit(0);
				}
			}
		});
	}

	public void connectToServer() {
		System.out.println("Connecting to server!");
		var addressDialog = new TextInputDialogBuilder()
				.setTitle("Server Address")
				.setDescription("Enter the address of the server to connect to. For example, \"localhost:1234\".")
				.setInitialContent("localhost:8123")
				.build();
		String address = addressDialog.showDialog(this.getTextGUI());
		if (address == null) return;
		String[] parts = address.split(":");
		if (parts.length != 2) return;
		String host = parts[0];
		int port = Integer.parseInt(parts[1]);

		var nameDialog = new TextInputDialogBuilder()
				.setTitle("Nickname")
				.setDescription("Enter a nickname to use while in the server.")
				.build();
		String nickname = nameDialog.showDialog(this.getTextGUI());
		if (nickname == null) return;

		try {
			var client = new ConcordClient(host, port, nickname);
			var chatPanel = new ServerPanel(client, this);
			client.getModel().addListener(chatPanel);
			new Thread(client).start();
			this.setComponent(chatPanel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
