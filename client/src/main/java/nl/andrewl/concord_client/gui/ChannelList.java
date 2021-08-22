package nl.andrewl.concord_client.gui;

import com.googlecode.lanterna.gui2.*;
import nl.andrewl.concord_client.ConcordClient;
import nl.andrewl.concord_core.msg.types.MoveToChannel;

import java.io.IOException;

public class ChannelList extends Panel {
	private final ConcordClient client;
	public ChannelList(ConcordClient client) {
		super(new LinearLayout(Direction.VERTICAL));
		this.client = client;
	}

	public void setChannels() {
		this.removeAllComponents();
		for (var channel : this.client.getServerMetaData().getChannels()) {
			String name = channel.getName();
			if (client.getCurrentChannelId().equals(channel.getId())) {
				name = "*" + name;
			}
			Button b = new Button(name, () -> {
				if (!client.getCurrentChannelId().equals(channel.getId())) {
					try {
						client.sendMessage(new MoveToChannel(channel.getId()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			this.addComponent(b, LinearLayout.createLayoutData(LinearLayout.Alignment.End));
		}
	}
}
