package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

/**
 * Metadata is sent by the server to clients to inform them of the structure of
 * the server. This includes basic information about the server's own properties
 * as well as information about all top-level channels.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerMetaData implements Message {
	private String name;
	private List<ChannelData> channels;

	@Override
	public int getByteCount() {
		return getByteSize(this.name) + getByteSize(this.channels);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeString(this.name, o);
		writeList(this.channels, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.name = readString(i);
		this.channels = readList(ChannelData.class, i);
	}

	/**
	 * Metadata about a top-level channel in the server which is visible and
	 * joinable for a user.
	 */
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ChannelData implements Message {
		private UUID id;
		private String name;

		@Override
		public int getByteCount() {
			return UUID_BYTES + getByteSize(this.name);
		}

		@Override
		public void write(DataOutputStream o) throws IOException {
			writeUUID(this.id, o);
			writeString(this.name, o);
		}

		@Override
		public void read(DataInputStream i) throws IOException {
			this.id = readUUID(i);
			this.name = readString(i);
		}
	}
}
