package org.maera.plugin.osgi.util;

import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.apache.commons.lang.Validate;
import org.maera.plugin.util.resource.AlternativeResourceLoader;
import org.maera.plugin.util.resource.NoOpAlternativeResourceLoader;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Utility methods for accessing a bundle as if it was a classloader.
 *
 * @since 2.3.0
 */
public class BundleClassLoaderAccessor {
    /**
     * Creates a classloader that delegates to the bundle
     *
     * @param bundle                    The bundle to delegate to
     * @param alternativeResourceLoader An alternative resource loader to bypass bundle, can be null
     * @return A new classloader instance
     */
    public static ClassLoader getClassLoader(final Bundle bundle, final AlternativeResourceLoader alternativeResourceLoader) {
        return new BundleClassLoader(bundle, alternativeResourceLoader);
    }

    /**
     * Loads a class from the bundle
     *
     * @param bundle The bundle
     * @param name   The name of the class to load
     * @param <T>    The type of the class
     * @return The class instance
     * @throws ClassNotFoundException If the class cannot be found in the bundle
     */
    public static <T> Class<T> loadClass(final Bundle bundle, final String name) throws ClassNotFoundException {
        Validate.notNull(bundle, "The bundle is required");
        @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
        final Class<T> loadedClass = bundle.loadClass(name);
        return loadedClass;
    }

    ///CLOVER:OFF

    /**
     * Fake classloader that delegates to a bundle
     */
    private static class BundleClassLoader extends ClassLoader {
        private final Bundle bundle;
        private final AlternativeResourceLoader altResourceLoader;

        public BundleClassLoader(final Bundle bundle, AlternativeResourceLoader altResourceLoader) {
            super(null);
            Validate.notNull(bundle, "The bundle must not be null");
            if (altResourceLoader == null) {
                altResourceLoader = new NoOpAlternativeResourceLoader();
            }
            this.altResourceLoader = altResourceLoader;
            this.bundle = bundle;

        }

        @Override
        public Class<?> findClass(final String name) throws ClassNotFoundException {
            return bundle.loadClass(name);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Enumeration<URL> findResources(final String name) throws IOException {
            Enumeration<URL> e = bundle.getResources(name);

            if (e == null) {
                e = new IteratorEnumeration(Collections.emptyList().iterator());
            } else {
                // For some reason, getResources() sometimes returns nothing, yet getResource() will return one.  This code
                // handles that strange case
                if (!e.hasMoreElements()) {
                    final URL resource = findResource(name);
                    if (resource != null) {
                        e = new IteratorEnumeration(Arrays.asList(resource).iterator());
                    }
                }
            }
            return e;
        }

        @Override
        public URL findResource(final String name) {
            URL url = altResourceLoader.getResource(name);
            if (url == null) {
                url = bundle.getResource(name);
            }
            return url;
        }
    }

}
