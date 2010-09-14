package org.maera.plugin.util.collect;

/**
 * Evaluate an input and return true or false. Useful for filtering.
 */
public interface Predicate<T> {
    boolean evaluate(T input);
}
