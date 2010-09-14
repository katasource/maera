package org.maera.plugin.module;

import org.maera.plugin.impl.AbstractPlugin;

import java.io.InputStream;
import java.net.URL;

public class MockContainerManagedPlugin extends AbstractPlugin implements ContainerManagedPlugin {
    private ContainerAccessor containerAccessor;

    public MockContainerManagedPlugin(ContainerAccessor containerAccessor) {
        this.containerAccessor = containerAccessor;
    }

    public ContainerAccessor getContainerAccessor() {
        return containerAccessor;
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
        return (Class<T>) Class.forName(clazz);
    }

    public ClassLoader getClassLoader() {
        return null;
    }

    public URL getResource(final String path) {
        return null;
    }

    public InputStream getResourceAsStream(final String name) {
        return null;
    }
}