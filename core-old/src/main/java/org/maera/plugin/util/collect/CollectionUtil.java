package org.maera.plugin.util.collect;

import java.util.*;

public class CollectionUtil {

    public static <T> void foreach(final Iterator<T> iterator, final Consumer<T> sink) {
        while (iterator.hasNext()) {
            sink.consume(iterator.next());
        }
    }

    public static <T> void foreach(final Iterable<T> iterable, final Consumer<T> sink) {
        if (iterable != null) {
            foreach(iterable.iterator(), sink);
        }
    }

    public static <T> List<T> toList(final Iterable<T> iterable) {
        return toList(iterable.iterator());
    }

    public static <T> List<T> toList(final Iterator<T> iterator) {
        final List<T> result = new ArrayList<T>();
        foreach(iterator, new Consumer<T>() {
            public void consume(final T element) {
                if (element != null) {
                    result.add(element);
                }
            }
        });
        return result;
    }

    public static <T, R> List<R> transform(final Iterator<T> iterator, final Function<T, R> transformer) {
        return toList(transformIterator(iterator, transformer));
    }

    public static <T, R> List<R> transform(final Iterable<T> iterable, final Function<T, R> transformer) {
        if (iterable == null) {
            return Collections.emptyList();
        }
        return transform(iterable.iterator(), transformer);
    }

    public static <T, R> Iterator<R> transformIterator(final Iterator<T> iterator, final Function<T, R> transformer) {
        return new TransformingIterator<T, R>(iterator, transformer);
    }

    /**
     * Create a filtered {@link Iterator}.
     *
     * @param <T>
     * @return
     */
    public static <T> Iterator<T> filter(final Iterator<T> iterator, final Predicate<T> predicate) {
        return new FilteredIterator<T>(iterator, predicate);
    }

    /**
     * Create a filtered {@link Iterator}.
     *
     * @param <T>
     * @return
     */
    public static <T> Iterable<T> filter(final Iterable<T> iterable, final Predicate<T> predicate) {
        return new FilteredIterable<T>(iterable, predicate);
    }

    static class FilteredIterable<T> implements Iterable<T> {
        private final Iterable<T> delegate;
        private final Predicate<T> predicate;

        FilteredIterable(final Iterable<T> delegate, final Predicate<T> predicate) {
            this.delegate = delegate;
            this.predicate = predicate;
        }

        public Iterator<T> iterator() {
            return new FilteredIterator<T>(delegate.iterator(), predicate);
        }

        @Override
        public String toString() {
            return toList(this).toString();
        }
    }

    public static <T> List<T> sort(final Collection<T> collection, final Comparator<T> comparator) {
        final List<T> sorted = new ArrayList<T>(collection);
        if (sorted.size() > 1) {
            Collections.sort(sorted, comparator);
        }
        return sorted;
    }
}
