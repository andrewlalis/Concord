package nl.andrewl.concord_core.msg.types;

import nl.andrewl.concord_core.msg.Message;

/**
 * Error message which can be sent between either the server or client to
 * indicate an unsavory situation.
 */
public record Error (
		Level level,
		String message
) implements Message {
	/**
	 * The error level gives an indication as to the severity of the error.
	 * Warnings indicate that a user has attempted to do something which they
	 * shouldn't, or some scenario which is not ideal but recoverable from.
	 * Errors indicate actual issues with the software which should be addressed.
	 */
	public enum Level {WARNING, ERROR}

	public static Error warning(String message) {
		return new Error(Level.WARNING, message);
	}

	public static Error error(String message) {
		return new Error(Level.ERROR, message);
	}
}
