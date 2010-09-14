package org.maera.plugin.loaders;

import org.maera.plugin.PluginArtifactFactory;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.factories.PluginFactory;

import java.io.File;
import java.util.List;

/**
 * A plugin loader to load plugins from a directory on disk.  A {@link DirectoryScanner} is used to locate plugin artifacts
 * and determine if they need to be redeployed or not.
 */
public class DirectoryPluginLoader extends ScanningPluginLoader {

    /**
     * Constructs a loader for a particular directory and set of deployers
     *
     * @param path               The directory containing the plugins
     * @param pluginFactories    The deployers that will handle turning an artifact into a plugin
     * @param pluginEventManager The event manager, used for listening for shutdown events
     * @since 2.0.0
     */
    public DirectoryPluginLoader(final File path, final List<PluginFactory> pluginFactories, final PluginEventManager pluginEventManager) {
        super(new DirectoryScanner(path), pluginFactories, pluginEventManager);
    }

    /**
     * Constructs a loader for a particular directory and set of deployers
     *
     * @param path                  The directory containing the plugins
     * @param pluginFactories       The deployers that will handle turning an artifact into a plugin
     * @param pluginArtifactFactory The plugin artifact factory
     * @param pluginEventManager    The event manager, used for listening for shutdown events
     * @since 2.1.0
     */
    public DirectoryPluginLoader(final File path, final List<PluginFactory> pluginFactories, final PluginArtifactFactory pluginArtifactFactory, final PluginEventManager pluginEventManager) {
        super(new DirectoryScanner(path), pluginFactories, pluginArtifactFactory, pluginEventManager);
    }
}
