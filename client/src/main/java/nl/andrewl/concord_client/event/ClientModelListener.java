package nl.andrewl.concord_client.event;

import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_core.msg.types.UserData;

import java.util.List;
import java.util.UUID;

public interface ClientModelListener {
	default void channelMoved(UUID oldChannelId, String oldChannelName, UUID newChannelId, String newChannelName) {}

	default void usersUpdated(List<UserData> users) {}

	default void serverMetaDataUpdated(ServerMetaData metaData) {}
}
