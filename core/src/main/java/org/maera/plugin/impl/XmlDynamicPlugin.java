package org.maera.plugin.impl;

import org.maera.plugin.util.ClassLoaderUtils;

import java.io.InputStream;
import java.net.URL;

/**
 * A dynamic XML plugin that consists of the Maera plugin descriptor
 *
 * @since 2.1.0
 */
public class XmlDynamicPlugin extends AbstractPlugin {

    public void close() {
    }

    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    public URL getResource(final String name) {
        return ClassLoaderUtils.getResource(name, getClass());
    }

    public InputStream getResourceAsStream(final String name) {
        return ClassLoaderUtils.getResourceAsStream(name, getClass());
    }

    public boolean isDeleteable() {
        return true;
    }

    public boolean isDynamicallyLoaded() {
        return true;
    }

    public boolean isUninstallable() {
        return true;
    }

    public <M> Class<M> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException {
        return ClassLoaderUtils.loadClass(clazz, callingClass);
    }
}
