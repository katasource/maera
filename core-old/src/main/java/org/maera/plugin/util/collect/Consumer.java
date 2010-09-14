package org.maera.plugin.util.collect;

/**
 * Consume the object a {@link Supplier} produces.
 */
public interface Consumer<T> {
    /**
     * Consume the product.
     *
     * @param element must not be null
     */
    void consume(T element);
}