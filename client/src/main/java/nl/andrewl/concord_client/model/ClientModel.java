package nl.andrewl.concord_client.model;

import lombok.Getter;
import nl.andrewl.concord_client.event.ClientModelListener;
import nl.andrewl.concord_core.msg.types.ServerMetaData;
import nl.andrewl.concord_core.msg.types.UserData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
public class ClientModel {
	private UUID id;
	private String nickname;
	private ServerMetaData serverMetaData;

	private UUID currentChannelId;
	private List<UserData> knownUsers;
	private final ChatHistory chatHistory;

	private final List<ClientModelListener> modelListeners;

	public ClientModel(UUID id, String nickname, UUID currentChannelId, ServerMetaData serverMetaData) {
		this.modelListeners = new CopyOnWriteArrayList<>();
		this.id = id;
		this.nickname = nickname;
		this.currentChannelId = currentChannelId;
		this.serverMetaData = serverMetaData;
		this.knownUsers = new ArrayList<>();
		this.chatHistory = new ChatHistory();
	}

	public void setCurrentChannelId(UUID newChannelId) {
		UUID oldId = this.currentChannelId;
		this.currentChannelId = newChannelId;
		this.modelListeners.forEach(listener -> listener.channelMoved(oldId, newChannelId));
	}

	public void setKnownUsers(List<UserData> users) {
		this.knownUsers = users;
		this.modelListeners.forEach(listener -> listener.usersUpdated(this.knownUsers));
	}

	public void setServerMetaData(ServerMetaData metaData) {
		this.serverMetaData = metaData;
		this.modelListeners.forEach(listener -> listener.serverMetaDataUpdated(metaData));
	}

	public void addListener(ClientModelListener listener) {
		this.modelListeners.add(listener);
	}

	public void removeListener(ClientModelListener listener) {
		this.modelListeners.remove(listener);
	}
}
