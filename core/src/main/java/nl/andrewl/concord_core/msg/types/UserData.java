package nl.andrewl.concord_core.msg.types;

import nl.andrewl.concord_core.msg.Message;

import java.util.UUID;

/**
 * Standard set of user data that is used mainly as a component of other more
 * complex messages.
 */
public record UserData (UUID id, String name) implements Message {}
