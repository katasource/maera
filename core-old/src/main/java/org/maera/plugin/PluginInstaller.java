package org.maera.plugin;

/**
 * A place to store plugins which can be installed and uninstalled.
 */
public interface PluginInstaller {
    /**
     * Installs the plugin with the given key. If the plugin already exists, it is replaced silently.
     */
    void installPlugin(String key, PluginArtifact pluginArtifact);
}
