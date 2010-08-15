package org.maera.plugin.loaders;

import org.apache.commons.lang.Validate;
import org.maera.plugin.*;
import org.maera.plugin.event.PluginEventListener;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginFrameworkShutdownEvent;
import org.maera.plugin.factories.PluginFactory;
import org.maera.plugin.impl.UnloadablePlugin;
import org.maera.plugin.loaders.classloading.DeploymentUnit;
import org.maera.plugin.loaders.classloading.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Plugin loader that delegates the detection of plugins to a Scanner instance. The scanner may monitor the contents
 * of a directory on disk, a database, or any other place plugins may be hidden.
 *
 * @since 2.1.0
 */
public class ScanningPluginLoader implements DynamicPluginLoader {
    private static final Logger log = LoggerFactory.getLogger(ScanningPluginLoader.class);

    protected final org.maera.plugin.loaders.classloading.Scanner scanner;
    protected final Map<DeploymentUnit, Plugin> plugins;
    protected final List<PluginFactory> pluginFactories;
    protected final PluginArtifactFactory pluginArtifactFactory;

    /**
     * Constructor that provides a default plugin artifact factory
     *
     * @param scanner            The scanner to use to detect new plugins
     * @param pluginFactories    The deployers that will handle turning an artifact into a plugin
     * @param pluginEventManager The event manager, used for listening for shutdown events
     * @since 2.0.0
     */
    public ScanningPluginLoader(final Scanner scanner, final List<PluginFactory> pluginFactories, final PluginEventManager pluginEventManager) {
        this(scanner, pluginFactories, new DefaultPluginArtifactFactory(), pluginEventManager);
    }

    /**
     * Construct a new scanning plugin loader with no default values
     *
     * @param scanner               The scanner to use to detect new plugins
     * @param pluginFactories       The deployers that will handle turning an artifact into a plugin
     * @param pluginArtifactFactory used to create new plugin artifacts from an URL
     * @param pluginEventManager    The event manager, used for listening for shutdown events
     * @since 2.0.0
     */
    public ScanningPluginLoader(final Scanner scanner, final List<PluginFactory> pluginFactories, final PluginArtifactFactory pluginArtifactFactory, final PluginEventManager pluginEventManager) {
        Validate.notNull(pluginFactories, "The list of plugin factories must be specified");
        Validate.notNull(pluginEventManager, "The event manager must be specified");
        Validate.notNull(scanner, "The scanner must be specified");

        plugins = new TreeMap<DeploymentUnit, Plugin>();

        this.pluginArtifactFactory = pluginArtifactFactory;
        this.scanner = scanner;
        this.pluginFactories = new ArrayList<PluginFactory>(pluginFactories);

        pluginEventManager.register(this);
    }

    public Collection<Plugin> loadAllPlugins(final ModuleDescriptorFactory moduleDescriptorFactory) {
        scanner.scan();

        for (final DeploymentUnit deploymentUnit : scanner.getDeploymentUnits()) {
            Plugin plugin = deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
            plugin = postProcess(plugin);
            plugins.put(deploymentUnit, plugin);
        }

        if (scanner.getDeploymentUnits().isEmpty()) {
            log.info("No plugins found to be deployed");
        }

        return Collections.unmodifiableCollection(new ArrayList<Plugin>(plugins.values()));
    }

    protected Plugin deployPluginFromUnit(final DeploymentUnit deploymentUnit, final ModuleDescriptorFactory moduleDescriptorFactory) {
        Plugin plugin = null;
        String errorText = "No plugin factories found for plugin file " + deploymentUnit;

        String pluginKey = null;
        for (final PluginFactory factory : pluginFactories) {
            try {
                final PluginArtifact artifact = pluginArtifactFactory.create(deploymentUnit.getPath().toURI());
                pluginKey = factory.canCreate(artifact);
                if (pluginKey != null) {
                    plugin = factory.create(artifact, moduleDescriptorFactory);
                    if (plugin != null) {
                        break;
                    }
                }
            }
            catch (final Throwable ex) {
                log.error("Unable to deploy plugin '" + pluginKey + "', file " + deploymentUnit, ex);
                errorText = ex.getMessage();
                break;
            }
        }
        if (plugin == null) {
            plugin = new UnloadablePlugin(errorText);
            if (pluginKey != null) {
                plugin.setKey(pluginKey);
            } else {
                plugin.setKey(deploymentUnit.getPath().getName());
            }

        } else {
            log.info("Plugin " + deploymentUnit + " created");
        }

        return plugin;
    }

    public boolean supportsRemoval() {
        return true;
    }

    public boolean supportsAddition() {
        return true;
    }

    /**
     * @return all plugins, now loaded by the pluginLoader, which have been discovered and added since the
     *         last time a check was performed.
     */
    public Collection<Plugin> addFoundPlugins(final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        // find missing plugins
        final Collection<DeploymentUnit> updatedDeploymentUnits = scanner.scan();

        // create list while updating internal state
        final List<Plugin> foundPlugins = new ArrayList<Plugin>();
        for (final DeploymentUnit deploymentUnit : updatedDeploymentUnits) {
            if (!plugins.containsKey(deploymentUnit)) {
                Plugin plugin = deployPluginFromUnit(deploymentUnit, moduleDescriptorFactory);
                plugin = postProcess(plugin);
                plugins.put(deploymentUnit, plugin);
                foundPlugins.add(plugin);
            }
        }
        if (foundPlugins.isEmpty()) {
            log.info("No plugins found to be installed");
        }

        return foundPlugins;
    }

    /**
     * @param plugin - the plugin to remove
     * @throws org.maera.plugin.PluginException
     *          representing the reason for failure.
     */
    public void removePlugin(final Plugin plugin) throws PluginException {
        if (plugin.getPluginState() == PluginState.ENABLED) {
            throw new PluginException("Cannot remove an enabled plugin");
        }

        if (!plugin.isUninstallable()) {
            throw new PluginException("Cannot remove an uninstallable plugin: [" + plugin.getName() + "]");
        }

        final DeploymentUnit deploymentUnit = findMatchingDeploymentUnit(plugin);
        plugin.uninstall();

        try {
            // Loop over to see if there are any other deployment units with the same filename. This will happen
            // if a newer plugin is uploaded with the same filename as the plugin being removed: in this case the
            // old one has already been deleted
            boolean found = false;
            for (final DeploymentUnit unit : plugins.keySet()) {
                if (unit.getPath().equals(deploymentUnit.getPath()) && !unit.equals(deploymentUnit)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                scanner.remove(deploymentUnit);
            }
        }
        catch (final SecurityException e) {
            throw new PluginException(e);
        }

        plugins.remove(deploymentUnit);
        log.info("Removed plugin " + plugin.getKey());
    }

    private DeploymentUnit findMatchingDeploymentUnit(final Plugin plugin) throws PluginException {
        DeploymentUnit deploymentUnit = null;
        for (final Map.Entry<DeploymentUnit, Plugin> entry : plugins.entrySet()) {
            // no, you don't want to use entry.getValue().equals(plugin) here as it breaks upgrades where it is a new
            // version of the plugin but the key and version number hasn't changed, and hence, equals() will always return
            // true
            if (entry.getValue() == plugin) {
                deploymentUnit = entry.getKey();
                break;
            }
        }

        if (deploymentUnit == null) {
            throw new PluginException("This pluginLoader has no memory of deploying the plugin you are trying remove: [" + plugin.getName() + "]");
        }
        return deploymentUnit;
    }

    /**
     * Called during plugin framework shutdown
     *
     * @param event The shutdown event
     */
    @PluginEventListener
    public void onShutdown(final PluginFrameworkShutdownEvent event) {
        for (final Iterator<Plugin> it = plugins.values().iterator(); it.hasNext();) {
            final Plugin plugin = it.next();
            if (plugin.isUninstallable()) {
                plugin.uninstall();
            }
            it.remove();
        }

        scanner.reset();
    }

    /**
     * @deprecated Since 2.0.0, shutdown will automatically occur when the plugin framework is shutdown
     */
    @Deprecated
    public void shutDown() {
        onShutdown(null);
    }

    /**
     * Determines if the artifact can be loaded by any of its deployers
     *
     * @param pluginArtifact The artifact to test
     * @return True if this artifact can be loaded by this loader
     * @throws org.maera.plugin.PluginParseException
     *
     */
    public String canLoad(final PluginArtifact pluginArtifact) throws PluginParseException {
        String pluginKey = null;
        for (final PluginFactory factory : pluginFactories) {
            pluginKey = factory.canCreate(pluginArtifact);
            if (pluginKey != null) {
                break;
            }
        }
        return pluginKey;
    }

    /**
     * Template method that can be used by a specific {@link PluginLoader} to
     * add information to a {@link Plugin} after it has been loaded.
     *
     * @param plugin a plugin that has been loaded
     * @since v2.2.0
     */
    protected Plugin postProcess(final Plugin plugin) {
        return plugin;
    }
}
