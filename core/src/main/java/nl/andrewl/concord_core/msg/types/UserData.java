package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import static nl.andrewl.concord_core.msg.MessageUtils.*;
import static nl.andrewl.concord_core.msg.MessageUtils.readString;

/**
 * Standard set of user data that is used mainly as a component of other more
 * complex messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserData implements Message {
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
