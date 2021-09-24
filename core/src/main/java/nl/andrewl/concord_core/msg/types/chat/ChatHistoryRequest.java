package nl.andrewl.concord_core.msg.types.chat;

import nl.andrewl.concord_core.msg.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
 *         <li><code>id</code> - A single message id to fetch. If this parameter
 *         is present, all others are ignored, and a list containing the single
 *         message is returned, if it could be found, otherwise an empty list.</li>
 *     </ul>
 * </p>
 * <p>
 *     Responses to this request are sent via {@link ChatHistoryResponse}, where
 *     the list of messages is always sorted by the timestamp.
 * </p>
 */
public record ChatHistoryRequest (UUID channelId, String query) implements Message {
	public ChatHistoryRequest(UUID channelId) {
		this(channelId, "");
	}

	public ChatHistoryRequest(UUID channelId, Map<String, String> params) {
		this(
			channelId,
			params.entrySet().stream()
				.map(entry -> {
					if (entry.getKey().contains(";") || entry.getKey().contains("=")) {
						throw new IllegalArgumentException("Parameter key \"" + entry.getKey() + "\" contains invalid characters.");
					}
					if (entry.getValue().contains(";") || entry.getValue().contains("=")) {
						throw new IllegalArgumentException("Parameter value \"" + entry.getValue() + "\" contains invalid characters.");
					}
					return entry.getKey() + "=" + entry.getValue();
				})
				.collect(Collectors.joining(";"))
		);
	}

	/**
	 * Utility method to extract the query string's values as a key-value map.
	 * @return A map of the query parameters.
	 */
	public Map<String, String> getQueryAsMap() {
		String[] pairs = this.query.split(";");
		if (pairs.length == 0) return Map.of();
		Map<String, String> params = new HashMap<>(pairs.length);
		for (var pair : pairs) {
			String[] keyAndValue = pair.split("=");
			if (keyAndValue.length != 2) continue;
			params.put(keyAndValue[0], keyAndValue[1]);
		}
		return params;
	}
}
