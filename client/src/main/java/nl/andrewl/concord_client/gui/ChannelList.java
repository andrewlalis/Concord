package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_core.msg.types.channel.MoveToChannel;

import java.io.IOException;

/**
 * Panel that contains a list of channels. A user can interact with a channel to
 * move to that channel. The current channel is indicated via a "*".
 */
public class ChannelList extends Panel {
	private final ConcordClient client;
	public ChannelList(ConcordClient client) {
		super(new LinearLayout(Direction.VERTICAL));
		this.client = client;
	}

	public void setChannels() {
		this.removeAllComponents();
		for (var channel : this.client.getModel().getServerMetaData().channels()) {
			String name = channel.name();
			if (client.getModel().getCurrentChannelId().equals(channel.id())) {
				name = "*" + name;
			}
			Button b = new Button(name, () -> {
				if (!client.getModel().getCurrentChannelId().equals(channel.id())) {
					try {
						client.sendMessage(new MoveToChannel(channel.id()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			this.addComponent(b, LinearLayout.createLayoutData(LinearLayout.Alignment.End));
		}
	}
}
