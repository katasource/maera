package org.maera.plugin.servlet;

import java.util.concurrent.atomic.AtomicReference;

public class ObjectFactories {
    private ObjectFactories() {
    }

    public static <T> ObjectFactory<T> createSingleton(final T object) {
        return new ObjectFactory<T>() {
            public T create() {
                return object;
            }
        };
    }

    public static <T> ObjectFactory<T> createMutable(final AtomicReference<T> ref) {
        return new ObjectFactory<T>() {
            public T create() {
                return ref.get();
            }
        };
    }
}
