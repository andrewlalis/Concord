package nl.andrewl.concord_core.msg.types;

import nl.andrewl.concord_core.msg.Message;

/**
 * This message is sent from the server to the client whenever a change happens
 * which requires the server to notify clients about a change of the list of
 * global users.
 */
public record ServerUsers (UserData[] users) implements Message {}
