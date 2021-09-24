package nl.andrewl.concord_core.msg.types.client_setup;

import nl.andrewl.concord_core.msg.Message;

/**
 * This message is sent as the first message from both the server and the client
 * to establish an end-to-end encryption via a key exchange.
 */
public record KeyData (byte[] iv, byte[] salt, byte[] publicKey) implements Message {}
