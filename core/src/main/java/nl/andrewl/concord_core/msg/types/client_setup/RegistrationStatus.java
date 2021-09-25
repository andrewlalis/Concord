package nl.andrewl.concord_core.msg.types.client_setup;

import nl.andrewl.concord_core.msg.Message;

/**
 * A response from the server which indicates the current status of the client's
 * registration request.
 */
public record RegistrationStatus (Type type) implements Message {
	public enum Type {PENDING, ACCEPTED, REJECTED}

	public static RegistrationStatus pending() {
		return new RegistrationStatus(Type.PENDING);
	}
}
