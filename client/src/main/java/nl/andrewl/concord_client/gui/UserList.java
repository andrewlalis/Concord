package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_core.msg.types.MoveToChannel;
import nl.andrewl.concord_core.msg.types.UserData;

import java.io.IOException;
import java.util.List;

public class UserList extends Panel {
	private final ConcordClient client;

	public UserList(ConcordClient client) {
		super(new LinearLayout(Direction.VERTICAL));
		this.client = client;
	}

	public void updateUsers(List<UserData> usersResponse) {
		this.removeAllComponents();
		for (var user : usersResponse) {
			Button b = new Button(user.getName(), () -> {
				if (!client.getModel().getId().equals(user.getId())) {
					System.out.println("Opening DM channel with user " + user.getName() + ", id: " + user.getId());
					try {
						client.sendMessage(new MoveToChannel(user.getId()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			this.addComponent(b);
		}
	}
}
