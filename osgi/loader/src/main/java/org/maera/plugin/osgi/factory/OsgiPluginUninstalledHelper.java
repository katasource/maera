package org.maera.plugin.osgi.factory;

import org.apache.commons.lang.Validate;
import org.maera.plugin.AutowireCapablePlugin;
import org.maera.plugin.IllegalPluginStateException;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.module.ContainerAccessor;
import org.maera.plugin.osgi.container.OsgiContainerException;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.util.OsgiHeaderUtil;
import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;

/**
 * Helper class that implements the methods assuming the OSGi plugin has not been installed
 *
 * @since 2.2.0
 */
class OsgiPluginUninstalledHelper implements OsgiPluginHelper {
    private final String key;
    private final OsgiContainerManager osgiContainerManager;
    private final PluginArtifact pluginArtifact;

    public OsgiPluginUninstalledHelper(String key, final OsgiContainerManager mgr, final PluginArtifact artifact) {
        Validate.notNull(key);
        Validate.notNull(mgr);
        Validate.notNull(artifact);
        this.key = key;
        this.pluginArtifact = artifact;
        this.osgiContainerManager = mgr;
    }

    public Bundle getBundle() {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException {
        throw new IllegalPluginStateException(getNotInstalledMessage() + " This is probably because the module " +
                "descriptor is trying to load classes in its init() method.  Move all classloading into the " +
                "enabled() method, and be sure to properly drop class and instance references in disabled().");
    }

    public URL getResource(String name) {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public InputStream getResourceAsStream(String name) {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public ClassLoader getClassLoader() {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public Bundle install() {
        Bundle bundle = osgiContainerManager.installBundle(pluginArtifact.toFile());
        if (!OsgiHeaderUtil.getPluginKey(bundle).equals(key)) {
            throw new IllegalArgumentException("The plugin key '" + key + "' must either match the OSGi bundle symbolic " +
                    "name (Bundle-SymbolicName) or be specified in the Maera-Plugin-Key manifest header");
        }
        return bundle;
    }

    public void onEnable(ServiceTracker... serviceTrackers) throws OsgiContainerException {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public void onDisable() throws OsgiContainerException {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public void onUninstall() throws OsgiContainerException {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public void autowire(Object instance, AutowireCapablePlugin.AutowireStrategy autowireStrategy) throws IllegalStateException {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public <T> T autowire(Class<T> clazz, AutowireCapablePlugin.AutowireStrategy autowireStrategy) throws IllegalPluginStateException {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public Set<String> getRequiredPlugins() {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public void setPluginContainer(Object container) {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    public ContainerAccessor getContainerAccessor() {
        throw new IllegalPluginStateException(getNotInstalledMessage());
    }

    private String getNotInstalledMessage() {
        return "This operation requires the plugin '" + key + "' to be installed";
    }
}
