package org.maera.plugin.util.concurrent;

import com.atlassian.util.concurrent.CopyOnWriteMaps;

import java.util.*;

/**
 * A thread-safe variant of {@link Map} in which all mutative operations (the
 * "destructive" operations described by {@link Map} put, remove and so on) are
 * implemented by making a fresh copy of the underlying map.
 * <p/>
 * This is ordinarily too costly, but may be <em>more</em> efficient than
 * alternatives when traversal operations vastly out-number mutations, and is
 * useful when you cannot or don't want to synchronize traversals, yet need to
 * preclude interference among concurrent threads. The "snapshot" style
 * iterators on the collections returned by {@link #entrySet()},
 * {@link #keySet()} and {@link #values()} use a reference to the internal map
 * at the point that the iterator was created. This map never changes during the
 * lifetime of the iterator, so interference is impossible and the iterator is
 * guaranteed not to throw <tt>ConcurrentModificationException</tt>. The
 * iterators will not reflect additions, removals, or changes to the list since
 * the iterator was created. Removing elements via these iterators is not
 * supported. The mutable operations on these collections (remove, retain etc.)
 * are supported but as with the {@link Map} interface, add and addAll are not
 * and throw {@link UnsupportedOperationException}.
 * <p/>
 * The actual copy is performed by an abstract {@link #copy(Map)} method. The
 * method is responsible for the underlying Map implementation (for instance a
 * {@link HashMap}, {@link TreeMap}, {@link LinkedHashMap} etc.) and therefore
 * the semantics of what this map will cope with as far as null keys and values,
 * iteration ordering etc. See the note below about suitable candidates for
 * underlying Map implementations
 * <p/>
 * There are supplied implementations for the common Collections {@link Map}
 * implementations via the {@link CopyOnWriteMaps} static factory methods.
 * <p/>
 * Collection views of the keys, values and entries are modifiable and will
 * cause a copy.
 * <p/>
 * <strong>Please note</strong> that the thread-safety guarantees are limited to
 * the thread-safety of the non-mutative (non-destructive) operations of the
 * underlying map implementation. For instance some implementations such as
 * {@link WeakHashMap} and {@link LinkedHashMap} with access ordering are
 * actually structurally modified by the {@link #get(Object)} method and are
 * therefore not suitable candidates as delegates for this class.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @deprecated since 2.5.0 please use the
 *             {@link com.atlassian.util.concurrent.CopyOnWriteMap} instead as
 *             it has some important features and some bug fixes. This version
 *             is no longer maintained.
 */
@Deprecated
public abstract class CopyOnWriteMap<K, V> extends com.atlassian.util.concurrent.CopyOnWriteMap<K, V> {
    private static final long serialVersionUID = 7935514534647505917L;

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link HashMap}.
     */
    public static <K, V> CopyOnWriteMap<K, V> newHashMap() {
        return new CopyOnWriteMap<K, V>() {
            private static final long serialVersionUID = 5221824943734164497L;

            @Override
            public <N extends Map<? extends K, ? extends V>> Map<K, V> copy(final N map) {
                return new HashMap<K, V>(map);
            }
        };
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying {@link HashMap}
     * using the supplied map as the initial values.
     */
    public static <K, V> CopyOnWriteMap<K, V> newHashMap(final Map<? extends K, ? extends V> map) {
        return new CopyOnWriteMap<K, V>(map) {
            private static final long serialVersionUID = -7616159260882572421L;

            @Override
            public <N extends Map<? extends K, ? extends V>> Map<K, V> copy(final N map) {
                return new HashMap<K, V>(map);
            }
        };
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying
     * {@link LinkedHashMap}. Iterators for this map will be return elements in
     * insertion order.
     */
    public static <K, V> CopyOnWriteMap<K, V> newLinkedMap() {
        return new CopyOnWriteMap<K, V>() {
            private static final long serialVersionUID = -4597421704607601676L;

            @Override
            public <N extends Map<? extends K, ? extends V>> Map<K, V> copy(final N map) {
                return new LinkedHashMap<K, V>(map);
            }
        };
    }

    /**
     * Creates a new {@link CopyOnWriteMap} with an underlying
     * {@link LinkedHashMap} using the supplied map as the initial values.
     * Iterators for this map will be return elements in insertion order.
     */
    public static <K, V> CopyOnWriteMap<K, V> newLinkedMap(final Map<? extends K, ? extends V> map) {
        return new CopyOnWriteMap<K, V>(map) {
            private static final long serialVersionUID = -8659999465009072124L;

            @Override
            public <N extends Map<? extends K, ? extends V>> Map<K, V> copy(final N map) {
                return new LinkedHashMap<K, V>(map);
            }
        };
    }

    //
    // constructors
    //

    /**
     * Create a new {@link CopyOnWriteMap} with the supplied {@link Map} to
     * initialize the values.
     *
     * @param map the initial map to initialize with
     */
    public CopyOnWriteMap(final Map<? extends K, ? extends V> map) {
        super(map);
    }

    /**
     * Create a new empty {@link CopyOnWriteMap}.
     */
    public CopyOnWriteMap() {
        super(Collections.<K, V>emptyMap());
    }
}