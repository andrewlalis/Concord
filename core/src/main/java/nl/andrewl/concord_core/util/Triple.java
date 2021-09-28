package nl.andrewl.concord_core.util;

/**
 * Simple generic triple of objects.
 * @param <A> The first object.
 * @param <B> The second object.
 * @param <C> The third object.
 */
public record Triple<A, B, C> (A first, B second, C third) {}
