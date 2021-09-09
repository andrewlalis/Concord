package nl.andrewl.concord_server.client;

/**
 * Exception that's thrown when a client's identification information is invalid.
 */
public class InvalidIdentificationException extends Exception {
	public InvalidIdentificationException(String message) {
		super(message);
	}
}
