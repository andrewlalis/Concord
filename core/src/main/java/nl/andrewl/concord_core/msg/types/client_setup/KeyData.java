package nl.andrewl.concord_core.msg.types.client_setup;

import nl.andrewl.concord_core.msg.Message;

/**
 * This message is sent as the first message from both the server and the client
 * to establish an end-to-end encryption via a key exchange.
 * @param iv The initialization vector bytes.
 * @param salt The salt bytes.
 * @param publicKey The public key.
 */
public record KeyData (byte[] iv, byte[] salt, byte[] publicKey) implements Message {}
