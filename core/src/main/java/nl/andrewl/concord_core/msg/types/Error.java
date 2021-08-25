package nl.andrewl.concord_core.msg.types;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nl.andrewl.concord_core.msg.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static nl.andrewl.concord_core.msg.MessageUtils.*;

/**
 * Error message which can be sent between either the server or client to
 * indicate an unsavory situation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Error implements Message {
	public enum Level {WARNING, ERROR}

	private Level level;
	private String message;

	public static Error warning(String message) {
		return new Error(Level.WARNING, message);
	}

	public static Error error(String message) {
		return new Error(Level.ERROR, message);
	}

	@Override
	public int getByteCount() {
		return Integer.BYTES + getByteSize(this.message);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeEnum(this.level, o);
		writeString(this.message, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.level = readEnum(Level.class, i);
		this.message = readString(i);
	}
}
