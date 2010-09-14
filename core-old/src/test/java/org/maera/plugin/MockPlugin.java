package org.maera.plugin;

import org.maera.plugin.impl.AbstractPlugin;

import java.io.InputStream;
import java.net.URL;

/**
 * @since 2.3.0
 */
public class MockPlugin extends AbstractPlugin {
    private ClassLoader classLoader;

    public MockPlugin(String key, ClassLoader classLoader) {
        setKey(key);
        setName(key);
        this.classLoader = classLoader;
    }

    public boolean isUninstallable() {
        return false;
    }

    public boolean isDeleteable() {
        return false;
    }

    public boolean isDynamicallyLoaded() {
        return false;
    }

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException {
        return null;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public URL getResource(final String path) {
        return null;
    }

    public InputStream getResourceAsStream(final String name) {
        return null;
    }
}
