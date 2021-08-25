package nl.andrewl.concord_client.event;

import nl.andrewl.concord_core.msg.types.ChannelUsersResponse;

import java.util.List;
import java.util.UUID;

public interface ClientModelListener {
	default void channelMoved(UUID oldChannelId, UUID newChannelId) {}

	default void usersUpdated(List<ChannelUsersResponse.UserData> users) {}
}
