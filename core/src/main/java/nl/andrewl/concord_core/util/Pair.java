package nl.andrewl.concord_core.util;

/**
 * Simple generic pair of two objects.
 * @param <A> The first object.
 * @param <B> The second object.
 */
public record Pair<A, B>(A first, B second) {}
