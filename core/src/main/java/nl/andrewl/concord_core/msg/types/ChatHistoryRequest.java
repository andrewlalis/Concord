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

/**
 * A message which clients can send to the server to request some messages from
 * the server's history of all sent messages from a particular source. Every
 * request must provide the id of the source that messages should be fetched
 * from, in addition to the type of source (channel, thread, dm).
 * <p>
 *     The query string is a specially-formatted string that allows you to
 *     filter results to only certain messages, using different parameters that
 *     are separated by the <code>;</code> character.
 * </p>
 * <p>
 *     All query parameters are of the form <code>param=value</code>, where
 *     <code>param</code> is the case-sensitive name of the parameter, and
 *     <code>value</code> is the value of the parameter.
 * </p>
 * <p>
 *     The following query parameters are supported:
 *     <ul>
 *         <li><code>count</code> - Fetch up to N messages. Minimum of 1, and
 *         a server-specific maximum count, usually no higher than 1000.</li>
 *         <li><code>from</code> - ISO-8601 timestamp indicating the timestamp
 *         after which messages should be fetched. Only messages after this
 *         point in time are returned.</li>
 *         <li><code>to</code> - ISO-8601 timestamp indicating the timestamp
 *         before which messages should be fetched. Only messages before this
 *         point in time are returned.</li>
 *     </ul>
 * </p>
 * <p>
 *     Responses to this request are sent via {@link ChatHistoryResponse}, where
 *     the list of messages is always sorted by the timestamp.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryRequest implements Message {
	public enum Source {CHANNEL, THREAD, DIRECT_MESSAGE}

	private UUID sourceId;
	private Source sourceType;
	private String query;

	@Override
	public int getByteCount() {
		return UUID_BYTES + Integer.BYTES + getByteSize(this.query);
	}

	@Override
	public void write(DataOutputStream o) throws IOException {
		writeUUID(this.sourceId, o);
		writeEnum(this.sourceType, o);
		writeString(this.query, o);
	}

	@Override
	public void read(DataInputStream i) throws IOException {
		this.sourceId = readUUID(i);
		this.sourceType = readEnum(Source.class, i);
		this.query = readString(i);
	}
}
