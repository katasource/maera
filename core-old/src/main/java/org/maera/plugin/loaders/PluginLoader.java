package org.maera.plugin.loaders;

import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginException;
import org.maera.plugin.PluginParseException;

import java.util.Collection;

/**
 * Handles loading and unloading plugin artifacts from a location
 */
public interface PluginLoader {
    Collection<Plugin> loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException;

    /**
     * @return a collection of discovered plugins which have now been loaded by this PluginLoader
     */
    Collection<Plugin> addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException;

    /**
     * @return true if this PluginLoader tracks whether or not plugins are added to it.
     */
    boolean supportsAddition();

    /**
     * @return true if this PluginLoader tracks whether or not plugins are removed from it.
     */
    boolean supportsRemoval();

    /**
     * Remove a specific plugin
     */
    void removePlugin(Plugin plugin) throws PluginException;
}
