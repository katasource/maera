package org.maera.plugin;

import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.loaders.PluginLoader;
import org.maera.plugin.manager.PluginPersistentStateStore;

import java.util.List;

/**
 * This implementation delegates the initiation and classloading of plugins to a
 * list of {@link PluginLoader}s and records the state of plugins in a {@link PluginPersistentStateStore}.
 * <p/>
 * This class is responsible for enabling and disabling plugins and plugin modules and reflecting these
 * state changes in the PluginStateStore.
 * <p/>
 * An interesting quirk in the design is that {@link #installPlugin(PluginArtifact)} explicitly stores
 * the plugin via a {@link PluginInstaller}, whereas {@link #uninstall(String)} relies on the
 * underlying {@link PluginLoader} to remove the plugin if necessary.
 *
 * @deprecated Since 2.2.0, use {@link org.maera.plugin.manager.DefaultPluginManager} instead
 */
@Deprecated
public class DefaultPluginManager extends org.maera.plugin.manager.DefaultPluginManager implements PluginManager {
    public DefaultPluginManager(final PluginPersistentStateStore store, final List<PluginLoader> pluginLoaders, final ModuleDescriptorFactory moduleDescriptorFactory, final PluginEventManager pluginEventManager) {
        super(store, pluginLoaders, moduleDescriptorFactory, pluginEventManager);
    }

}
