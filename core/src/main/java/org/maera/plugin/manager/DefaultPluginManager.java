package org.maera.plugin.manager;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.apache.commons.lang.time.StopWatch;
import org.maera.plugin.*;
import org.maera.plugin.classloader.PluginsClassLoader;
import org.maera.plugin.descriptors.CannotDisable;
import org.maera.plugin.descriptors.UnloadableModuleDescriptor;
import org.maera.plugin.descriptors.UnloadableModuleDescriptorFactory;
import org.maera.plugin.event.NotificationException;
import org.maera.plugin.event.PluginEventListener;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.*;
import org.maera.plugin.impl.UnloadablePlugin;
import org.maera.plugin.impl.UnloadablePluginFactory;
import org.maera.plugin.loaders.DynamicPluginLoader;
import org.maera.plugin.loaders.PluginLoader;
import org.maera.plugin.manager.PluginPersistentState.Builder;
import org.maera.plugin.parsers.DescriptorParserFactory;
import org.maera.plugin.predicate.*;
import org.maera.plugin.util.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Maps.filterKeys;
import static org.maera.plugin.util.Assertions.notNull;
import static org.maera.plugin.util.collect.CollectionUtil.toList;

/**
 * This implementation delegates the initiation and classloading of plugins to a
 * list of {@link org.maera.plugin.loaders.PluginLoader}s and records the
 * state of plugins in a
 * {@link org.maera.plugin.manager.PluginPersistentStateStore}.
 * <p/>
 * This class is responsible for enabling and disabling plugins and plugin
 * modules and reflecting these state changes in the PluginPersistentStateStore.
 * <p/>
 * An interesting quirk in the design is that
 * {@link #installPlugin(org.maera.plugin.PluginArtifact)} explicitly stores
 * the plugin via a {@link org.maera.plugin.PluginInstaller}, whereas
 * {@link #uninstall(Plugin)} relies on the underlying
 * {@link org.maera.plugin.loaders.PluginLoader} to remove the plugin if
 * necessary.
 */
public class DefaultPluginManager implements PluginController, PluginAccessor, PluginSystemLifecycle {
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginManager.class);

    private final List<PluginLoader> pluginLoaders;
    private final PluginPersistentStateStore store;
    private final ModuleDescriptorFactory moduleDescriptorFactory;
    private final PluginEventManager pluginEventManager;

    private final Map<String, Plugin> plugins = CopyOnWriteMap.<String, Plugin>builder().stableViews().newHashMap();
    private final PluginsClassLoader classLoader;
    private final PluginEnabler pluginEnabler = new PluginEnabler(this, this);
    private final StateTracker tracker = new StateTracker();

    /**
     * Installer used for storing plugins. Used by
     * {@link #installPlugin(PluginArtifact)}.
     */
    private RevertablePluginInstaller pluginInstaller = new NoOpRevertablePluginInstaller(new UnsupportedPluginInstaller());

    /**
     * Stores {@link Plugin}s as a key and {@link PluginLoader} as a value.
     */
    private final Map<Plugin, PluginLoader> pluginToPluginLoader = new HashMap<Plugin, PluginLoader>();

    public DefaultPluginManager(final PluginPersistentStateStore store, final List<PluginLoader> pluginLoaders, final ModuleDescriptorFactory moduleDescriptorFactory,
                                final PluginEventManager pluginEventManager) {
        this.pluginLoaders = notNull("Plugin Loaders list must not be null.", pluginLoaders);
        this.store = notNull("PluginPersistentStateStore must not be null.", store);
        this.moduleDescriptorFactory = notNull("ModuleDescriptorFactory must not be null.", moduleDescriptorFactory);
        this.pluginEventManager = notNull("PluginEventManager must not be null.", pluginEventManager);

        this.pluginEventManager.register(this);
        classLoader = new PluginsClassLoader(null, this, pluginEventManager);
    }

    public void init() throws PluginParseException, NotificationException {
        tracker.setState(StateTracker.State.STARTING);
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("Initialising the plugin system");
        pluginEventManager.broadcast(new PluginFrameworkStartingEvent(this, this));
        pluginInstaller.clearBackups();
        for (final PluginLoader loader : pluginLoaders) {
            if (loader == null) {
                continue;
            }

            final Iterable<Plugin> possiblePluginsToLoad = loader.loadAllPlugins(moduleDescriptorFactory);
            final Collection<Plugin> pluginsToLoad = new ArrayList<Plugin>();
            for (final Plugin plugin : possiblePluginsToLoad) {
                if (getState().getPluginRestartState(plugin.getKey()) == PluginRestartState.REMOVE) {
                    log.info("Plugin " + plugin.getKey() + " was marked to be removed on restart.  Removing now.");
                    loader.removePlugin(plugin);

                    // PLUG-13: Plugins should not save state across uninstalls.
                    removeStateFromStore(getStore(), plugin);
                } else {
                    pluginsToLoad.add(plugin);
                }
            }
            addPlugins(loader, pluginsToLoad);
        }

        getStore().save(getBuilder().clearPluginRestartState().toState());

        pluginEventManager.broadcast(new PluginFrameworkStartedEvent(this, this));
        stopWatch.stop();
        log.info("Plugin system started in " + stopWatch);
        tracker.setState(StateTracker.State.STARTED);
    }

    /**
     * Fires the shutdown event
     *
     * @throws IllegalStateException if already shutdown or already in the
     *                               process of shutting down.
     * @since 2.0.0
     */
    public void shutdown() {
        tracker.setState(StateTracker.State.SHUTTING_DOWN);
        log.info("Shutting down the plugin system");
        try {
            pluginEventManager.broadcast(new PluginFrameworkShutdownEvent(this, this));
        }
        catch (final NotificationException ex) {
            log.error("At least one error occured while broadcasting the PluginFrameworkShutdownEvent. We will continue to shutdown the Plugin Manager anyway.");
        }
        plugins.clear();
        pluginEventManager.unregister(this);
        tracker.setState(StateTracker.State.SHUTDOWN);
    }

    public final void warmRestart() {
        tracker.setState(StateTracker.State.WARM_RESTARTING);
        log.info("Initiating a warm restart of the plugin system");
        pluginEventManager.broadcast(new PluginFrameworkWarmRestartingEvent(this, this));

        // Make sure we reload plugins in order
        final List<Plugin> restartedPlugins = new ArrayList<Plugin>();
        final List<PluginLoader> loaders = new ArrayList<PluginLoader>(pluginLoaders);
        Collections.reverse(loaders);
        for (final PluginLoader loader : pluginLoaders) {
            for (final Map.Entry<Plugin, PluginLoader> entry : pluginToPluginLoader.entrySet()) {
                if (entry.getValue() == loader) {
                    final Plugin plugin = entry.getKey();
                    if (isPluginEnabled(plugin.getKey())) {
                        disablePluginModules(plugin);
                        restartedPlugins.add(plugin);
                    }
                }
            }
        }

        // then enable them in reverse order
        Collections.reverse(restartedPlugins);
        for (final Plugin plugin : restartedPlugins) {
            enableConfiguredPluginModules(plugin);
        }

        pluginEventManager.broadcast(new PluginFrameworkWarmRestartedEvent(this, this));
        tracker.setState(StateTracker.State.STARTED);
    }

    @PluginEventListener
    public void onPluginModuleAvailable(PluginModuleAvailableEvent event) {
        enableConfiguredPluginModule(event.getModule().getPlugin(), event.getModule(), new HashSet());
    }

    @PluginEventListener
    public void onPluginModuleUnavailable(PluginModuleUnavailableEvent event) {
        notifyModuleDisabled(event.getModule());
    }

    @PluginEventListener
    public void onPluginContainerUnavailable(PluginContainerUnavailableEvent event) {
        disablePluginWithoutPersisting(event.getPluginKey());
    }

    @PluginEventListener
    public void onPluginRefresh(final PluginRefreshedEvent event) {
        final Plugin plugin = event.getPlugin();

        disablePluginModules(plugin);

        // enable the plugin, shamefully copied from notifyPluginEnabled()
        if (enableConfiguredPluginModules(plugin)) {
            pluginEventManager.broadcast(new PluginEnabledEvent(plugin));
        }
    }

    /**
     * Set the plugin installation strategy for this manager
     *
     * @param pluginInstaller the plugin installation strategy to use
     * @see PluginInstaller
     */
    public void setPluginInstaller(final PluginInstaller pluginInstaller) {
        if (pluginInstaller instanceof RevertablePluginInstaller) {
            this.pluginInstaller = (RevertablePluginInstaller) pluginInstaller;
        } else {
            this.pluginInstaller = new NoOpRevertablePluginInstaller(pluginInstaller);
        }
    }

    protected final PluginPersistentStateStore getStore() {
        return store;
    }

    public String installPlugin(final PluginArtifact pluginArtifact) throws PluginParseException {
        final Set<String> keys = installPlugins(pluginArtifact);
        if ((keys != null) && (keys.size() == 1)) {
            return keys.iterator().next();
        } else {
            // should never happen
            throw new PluginParseException("Could not install plugin");
        }
    }

    public Set<String> installPlugins(final PluginArtifact... pluginArtifacts) throws PluginParseException {
        final Map<String, PluginArtifact> validatedArtifacts = new LinkedHashMap<String, PluginArtifact>();
        try {
            for (final PluginArtifact pluginArtifact : pluginArtifacts) {
                validatedArtifacts.put(validatePlugin(pluginArtifact), pluginArtifact);
            }
        }
        catch (final PluginParseException ex) {
            throw new PluginParseException("All plugins could not be validated", ex);
        }

        for (final Map.Entry<String, PluginArtifact> entry : validatedArtifacts.entrySet()) {
            pluginInstaller.installPlugin(entry.getKey(), entry.getValue());
        }

        scanForNewPlugins();
        return validatedArtifacts.keySet();
    }

    /**
     * Validate a plugin jar. Looks through all plugin loaders for ones that can
     * load the plugin and extract the plugin key as proof.
     *
     * @param pluginArtifact the jar file representing the plugin
     * @return The plugin key
     * @throws PluginParseException if the plugin cannot be parsed
     * @throws NullPointerException if <code>pluginJar</code> is null.
     */
    String validatePlugin(final PluginArtifact pluginArtifact) throws PluginParseException {
        boolean foundADynamicPluginLoader = false;
        for (final PluginLoader loader : pluginLoaders) {
            if (loader instanceof DynamicPluginLoader) {
                foundADynamicPluginLoader = true;
                final String key = ((DynamicPluginLoader) loader).canLoad(pluginArtifact);
                if (key != null) {
                    return key;
                }
            }
        }

        if (!foundADynamicPluginLoader) {
            throw new IllegalStateException("Should be at least one DynamicPluginLoader in the plugin loader list");
        }
        throw new PluginParseException("Jar " + pluginArtifact.getName() + " is not a valid plugin");
    }

    public int scanForNewPlugins() throws PluginParseException {
        int numberFound = 0;

        for (final PluginLoader loader : pluginLoaders) {
            if (loader != null) {
                if (loader.supportsAddition()) {
                    final List<Plugin> pluginsToAdd = new ArrayList<Plugin>();
                    for (Plugin plugin : loader.addFoundPlugins(moduleDescriptorFactory)) {
                        final Plugin oldPlugin = plugins.get(plugin.getKey());
                        // Only actually install the plugin if its module
                        // descriptors support it. Otherwise, mark it as
                        // unloadable.
                        if (!(plugin instanceof UnloadablePlugin)) {
                            if (PluginUtils.doesPluginRequireRestart(plugin)) {
                                if (oldPlugin == null) {
                                    markPluginInstallThatRequiresRestart(plugin);

                                    final UnloadablePlugin unloadablePlugin = UnloadablePluginFactory.createUnloadablePlugin(plugin);
                                    unloadablePlugin.setErrorText("Plugin requires a restart of the application due " +
                                            "to the following modules: " + PluginUtils.getPluginModulesThatRequireRestart(plugin));
                                    plugin = unloadablePlugin;
                                } else {
                                    markPluginUpgradeThatRequiresRestart(plugin);
                                    continue;
                                }
                            }

                            // Check to ensure that the old plugin didn't
                            // require restart, even if the new one doesn't
                            else if ((oldPlugin != null) && PluginUtils.doesPluginRequireRestart(oldPlugin)) {
                                markPluginUpgradeThatRequiresRestart(plugin);
                                continue;
                            }
                            pluginsToAdd.add(plugin);
                        }
                    }
                    addPlugins(loader, pluginsToAdd);
                    numberFound = pluginsToAdd.size();
                }
            }
        }
        return numberFound;
    }

    private void markPluginInstallThatRequiresRestart(Plugin plugin) {
        log.info("Installed plugin '" + plugin.getKey() + "' requires a restart due to the following modules: " +
                PluginUtils.getPluginModulesThatRequireRestart(plugin));
        getStore().save(getBuilder().setPluginRestartState(plugin.getKey(), PluginRestartState.INSTALL).toState());
    }

    private void markPluginUpgradeThatRequiresRestart(Plugin plugin) {
        log.info("Upgraded plugin '" + plugin.getKey() + "' requires a restart due to the following modules: " +
                PluginUtils.getPluginModulesThatRequireRestart(plugin));
        getStore().save(getBuilder().setPluginRestartState(plugin.getKey(), PluginRestartState.UPGRADE).toState());
    }

    private void markPluginUninstallThatRequiresRestart(Plugin plugin) {
        log.info("Uninstalled plugin '" + plugin.getKey() + "' requires a restart due to the following modules: " +
                PluginUtils.getPluginModulesThatRequireRestart(plugin));
        getStore().save(getBuilder().setPluginRestartState(plugin.getKey(), PluginRestartState.REMOVE).toState());
    }

    /**
     * @param plugin
     * @throws PluginException If the plugin or loader doesn't support
     *                         uninstallation
     */
    public void uninstall(final Plugin plugin) throws PluginException {
        if (PluginUtils.doesPluginRequireRestart(plugin)) {
            ensurePluginAndLoaderSupportsUninstall(plugin);
            markPluginUninstallThatRequiresRestart(plugin);
        } else {
            // Explicitly disable any plugins that require this plugin
            disableDependentPlugins(plugin);

            uninstallNoEvent(plugin);

            pluginEventManager.broadcast(new PluginUninstalledEvent(plugin));
        }
    }

    /**
     * Preforms an uninstallation without broadcasting the uninstallation event.
     *
     * @param plugin The plugin to uninstall
     * @since 2.5.0
     */
    protected void uninstallNoEvent(Plugin plugin) {
        unloadPlugin(plugin);

        // PLUG-13: Plugins should not save state across uninstalls.
        removeStateFromStore(getStore(), plugin);
    }

    /**
     * @param pluginKey The plugin key to revert
     * @throws PluginException If the revert cannot be completed
     */
    public void revertRestartRequiredChange(final String pluginKey) throws PluginException {
        notNull("pluginKey", pluginKey);
        PluginRestartState restartState = getState().getPluginRestartState(pluginKey);
        if (restartState == PluginRestartState.UPGRADE) {
            pluginInstaller.revertInstalledPlugin(pluginKey);
        } else if (restartState == PluginRestartState.INSTALL) {
            pluginInstaller.revertInstalledPlugin(pluginKey);
            plugins.remove(pluginKey);
        }
        getStore().save(getBuilder().setPluginRestartState(pluginKey, PluginRestartState.NONE).toState());
    }

    protected void removeStateFromStore(final PluginPersistentStateStore stateStore, final Plugin plugin) {
        final PluginPersistentState.Builder builder = PluginPersistentState.Builder.create(stateStore.load()).removeState(plugin.getKey());
        for (final ModuleDescriptor<?> moduleDescriptor : plugin.getModuleDescriptors()) {
            builder.removeState(moduleDescriptor.getCompleteKey());
        }
        stateStore.save(builder.toState());
    }

    /**
     * Unload a plugin. Called when plugins are added locally, or remotely in a
     * clustered application.
     *
     * @param plugin the plugin to remove
     * @throws PluginException if the plugin cannot be uninstalled
     */
    protected void unloadPlugin(final Plugin plugin) throws PluginException {
        final PluginLoader loader = ensurePluginAndLoaderSupportsUninstall(plugin);

        if (isPluginEnabled(plugin.getKey())) {
            notifyPluginDisabled(plugin);
        }

        notifyUninstallPlugin(plugin);
        if (loader != null) {
            removePluginFromLoader(plugin);
        }

        plugins.remove(plugin.getKey());
    }

    private PluginLoader ensurePluginAndLoaderSupportsUninstall(final Plugin plugin) {
        if (!plugin.isUninstallable()) {
            throw new PluginException("Plugin is not uninstallable: " + plugin.getKey());
        }

        final PluginLoader loader = pluginToPluginLoader.get(plugin);

        if ((loader != null) && !loader.supportsRemoval()) {
            throw new PluginException("Not uninstalling plugin - loader doesn't allow removal. Plugin: " + plugin.getKey());
        }
        return loader;
    }

    private void removePluginFromLoader(final Plugin plugin) throws PluginException {
        if (plugin.isDeleteable()) {
            final PluginLoader pluginLoader = pluginToPluginLoader.get(plugin);
            pluginLoader.removePlugin(plugin);
        }

        pluginToPluginLoader.remove(plugin);
    }

    protected void notifyUninstallPlugin(final Plugin plugin) {
        classLoader.notifyUninstallPlugin(plugin);

        for (final ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors()) {
            descriptor.destroy(plugin);
        }
    }

    protected PluginPersistentState getState() {
        return getStore().load();
    }

    /**
     * @deprecated Since 2.0.2, use {@link
     *             #addPlugins(PluginLoader,Collection<Plugin>...)} instead
     */
    @Deprecated
    protected void addPlugin(final PluginLoader loader, final Plugin plugin) throws PluginParseException {
        addPlugins(loader, Collections.singletonList(plugin));
    }

    /**
     * Update the local plugin state and enable state aware modules.
     * <p/>
     * If there is an existing plugin with the same key, the version strings of
     * the existing plugin and the plugin provided to this method will be parsed
     * and compared. If the installed version is newer than the provided
     * version, it will not be changed. If the specified plugin's version is the
     * same or newer, the existing plugin state will be saved and the plugin
     * will be unloaded before the provided plugin is installed. If the existing
     * plugin cannot be unloaded a {@link PluginException} will be thrown.
     *
     * @param loader           the loader used to load this plugin
     * @param pluginsToInstall the plugins to add
     * @throws PluginParseException if the plugin cannot be parsed
     * @since 2.0.2
     */
    protected void addPlugins(final PluginLoader loader, final Collection<Plugin> pluginsToInstall) throws PluginParseException {
        final Set<Plugin> pluginsToEnable = new HashSet<Plugin>();

        // Install plugins, looking for upgrades and duplicates
        for (final Plugin plugin : new TreeSet<Plugin>(pluginsToInstall)) {
            boolean pluginUpgraded = false;
            // testing to make sure plugin keys are unique
            final Plugin existingPlugin = plugins.get(plugin.getKey());
            if (existingPlugin != null) {
                if (plugin.compareTo(existingPlugin) >= 0) {
                    try {
                        updatePlugin(existingPlugin, plugin);
                        pluginsToEnable.remove(existingPlugin);
                        pluginUpgraded = true;
                    }
                    catch (final PluginException e) {
                        throw new PluginParseException("Duplicate plugin found (installed version is the same or older) and could not be unloaded: '" + plugin.getKey() + "'", e);
                    }
                } else {
                    // If we find an older plugin, don't error, just ignore it.
                    // PLUG-12.
                    if (log.isDebugEnabled()) {
                        log.debug("Duplicate plugin found (installed version is newer): '" + plugin.getKey() + "'");
                    }
                    // and don't install the older plugin
                    continue;
                }
            }

            plugin.install();
            final boolean isPluginEnabled = getState().isEnabled(plugin);
            if (isPluginEnabled) {
                pluginsToEnable.add(plugin);
            }
            if (plugin.isSystemPlugin() && !isPluginEnabled) {
                log.warn("System plugin is disabled: " + plugin.getKey());
            }
            if (pluginUpgraded) {
                pluginsToEnable.addAll(disableDependentPlugins(plugin));
                pluginEventManager.broadcast(new PluginUpgradedEvent(plugin));
            }
            plugins.put(plugin.getKey(), plugin);
            pluginToPluginLoader.put(plugin, loader);
        }

        // enable all plugins, waiting a time period for them to enable
        pluginEnabler.enable(pluginsToEnable);

        // handle the plugins that were able to be successfully enabled
        for (final Plugin plugin : pluginsToInstall) {
            if (plugin.getPluginState() == PluginState.ENABLED) {
                // This method enables the plugin modules
                if (enableConfiguredPluginModules(plugin)) {
                    pluginEventManager.broadcast(new PluginEnabledEvent(plugin));
                }
            }
        }
    }

    /**
     * Disables all dependent plugins to prevent a dependent plugin trying to access, indirectly,
     * the felix global lock, which is held by the PackageAdmin while refreshing.
     * see http://studio.atlassian.com/browse/PLUG-582
     *
     * @param plugin The plugin to disable
     * @return A set of plugins that were disabled
     */
    private Set<Plugin> disableDependentPlugins(Plugin plugin) {
        Set<Plugin> dependentPlugins = new HashSet<Plugin>();
        Set<String> dependentPluginKeys = new HashSet<String>();

        for (Plugin depPlugin : getEnabledPlugins()) {
            if (plugin != depPlugin && depPlugin.getRequiredPlugins().contains(plugin.getKey())) {
                dependentPlugins.add(depPlugin);
                dependentPluginKeys.add(depPlugin.getKey());
            }
        }
        log.info("Found dependent enabled plugins for uninstalled plugin '" + plugin.getKey() + "': " + dependentPluginKeys
                + ".  Disabling...");
        for (Plugin depPlugin : dependentPlugins) {
            disablePluginWithoutPersisting(depPlugin.getKey());
        }
        return dependentPlugins;
    }

    /**
     * Replace an already loaded plugin with another version. Relevant stored
     * configuration for the plugin will be preserved.
     *
     * @param oldPlugin Plugin to replace
     * @param newPlugin New plugin to install
     * @throws PluginException if the plugin cannot be updated
     */
    protected void updatePlugin(final Plugin oldPlugin, final Plugin newPlugin) throws PluginException {
        if (!oldPlugin.getKey().equals(newPlugin.getKey())) {
            throw new IllegalArgumentException("New plugin must have the same key as the old plugin");
        }

        if (log.isInfoEnabled()) {
            log.info("Updating plugin '" + oldPlugin + "' to '" + newPlugin + "'");
        }

        // Preserve the old plugin configuration - uninstall changes it (as
        // disable is called on all modules) and then
        // removes it
        final Map<String, Boolean> oldPluginState = new HashMap<String, Boolean>(getState().getPluginStateMap(oldPlugin));

        if (log.isDebugEnabled()) {
            log.debug("Uninstalling old plugin: " + oldPlugin);
        }
        uninstallNoEvent(oldPlugin);
        if (log.isDebugEnabled()) {
            log.debug("Plugin uninstalled '" + oldPlugin + "', preserving old state");
        }

        // Build a set of module keys from the new plugin version
        final Set<String> newModuleKeys = new HashSet<String>();
        newModuleKeys.add(newPlugin.getKey());
        for (final ModuleDescriptor<?> moduleDescriptor : newPlugin.getModuleDescriptors()) {
            newModuleKeys.add(moduleDescriptor.getCompleteKey());
        }

        // for removing any keys from the old plugin state that do not exist in
        // the
        // new version
        final Predicate<String> filter = new Predicate<String>() {
            public boolean apply(final String o) {
                return newModuleKeys.contains(o);
            }
        };

        getStore().save(getBuilder().addState(filterKeys(oldPluginState, filter)).toState());
    }

    public Collection<Plugin> getPlugins() {
        return plugins.values();
    }

    /**
     * @see PluginAccessor#getPlugins(org.maera.plugin.predicate.PluginPredicate)
     * @since 0.17
     */
    public Collection<Plugin> getPlugins(final PluginPredicate pluginPredicate) {
        return toList(filter(getPlugins(), new Predicate<Plugin>() {
            public boolean apply(final Plugin plugin) {
                return pluginPredicate.matches(plugin);
            }
        }));
    }

    /**
     * @see PluginAccessor#getEnabledPlugins()
     */
    public Collection<Plugin> getEnabledPlugins() {
        return getPlugins(new EnabledPluginPredicate(this));
    }

    /**
     * @see PluginAccessor#getModules(org.maera.plugin.predicate.ModuleDescriptorPredicate)
     * @since 0.17
     */
    public <M> Collection<M> getModules(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate) {
        return toList(getModules(getModuleDescriptors(moduleDescriptorPredicate)));
    }

    /**
     * @see PluginAccessor#getModuleDescriptors(org.maera.plugin.predicate.ModuleDescriptorPredicate)
     * @since 0.17
     */
    public <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate) {
        return toList(getModuleDescriptors(getPlugins(), moduleDescriptorPredicate));
    }

    /**
     * Get the all the module descriptors from the given collection of plugins,
     * filtered by the predicate.
     * <p/>
     * Be careful, your predicate must filter ModuleDescriptors that are not M,
     * this method does not guarantee that the descriptors are of the correct
     * type by itself.
     *
     * @param plugins a collection of {@link Plugin}s
     * @return a collection of {@link ModuleDescriptor descriptors}
     */
    private <M> Iterable<ModuleDescriptor<M>> getModuleDescriptors(final Collection<Plugin> plugins, final ModuleDescriptorPredicate<M> predicate) {
        // hack way to get typed descriptors from plugin and
        // keep generics happy
        final Function<ModuleDescriptor<?>, ModuleDescriptor<M>> coercer = new Function<ModuleDescriptor<?>, ModuleDescriptor<M>>() {
            public ModuleDescriptor<M> apply(final ModuleDescriptor<?> input) {
                @SuppressWarnings("unchecked")
                final ModuleDescriptor<M> result = (ModuleDescriptor<M>) input;
                return result;
            }
        };

        // google predicate adapter
        final Predicate<ModuleDescriptor<M>> adapter = new Predicate<ModuleDescriptor<M>>() {
            public boolean apply(final ModuleDescriptor<M> input) {
                return predicate.matches(input);
            }
        };

        // get the filtered module descriptors from a plugin
        final Function<Plugin, Iterable<ModuleDescriptor<M>>> descriptorExtractor = new Function<Plugin, Iterable<ModuleDescriptor<M>>>() {
            public Iterable<ModuleDescriptor<M>> apply(final Plugin plugin) {
                return filter(transform(plugin.getModuleDescriptors(), coercer), adapter);
            }
        };

        // concatenate all the descriptor iterables into one
        return concat(transform(plugins, descriptorExtractor));
    }

    /**
     * Get the modules of all the given descriptor. If any of the getModule()
     * calls fails, the error is recorded in the logs and the plugin is
     * disabled.
     *
     * @param moduleDescriptors the collection of module descriptors to get the
     *                          modules from.
     * @return a {@link Collection} modules that can be any type of object. This
     *         collection will not contain any null value.
     */
    private <M> List<M> getModules(final Iterable<ModuleDescriptor<M>> moduleDescriptors) {
        final Set<String> pluginsToDisable = new HashSet<String>();
        try {
            return toList(transform(moduleDescriptors, new Function<ModuleDescriptor<M>, M>() {
                public M apply(final ModuleDescriptor<M> input) {
                    M result = null;
                    try {
                        result = input.getModule();
                    }
                    catch (final RuntimeException ex) {
                        log.error("Exception when retrieving plugin module " + input.getKey() + ", will disable plugin " + input.getPlugin().getKey(), ex);
                        pluginsToDisable.add(input.getPlugin().getKey());
                    }
                    return result;
                }
            }));
        }
        finally {
            for (final String badPluginKey : pluginsToDisable) {
                disablePlugin(badPluginKey);
            }
        }
    }

    public Plugin getPlugin(final String key) {
        return plugins.get(notNull("The plugin key must be specified", key));
    }

    public Plugin getEnabledPlugin(final String pluginKey) {
        if (!isPluginEnabled(pluginKey)) {
            return null;
        }
        return getPlugin(pluginKey);
    }

    public ModuleDescriptor<?> getPluginModule(final String completeKey) {
        return getPluginModule(new ModuleCompleteKey(completeKey));
    }

    private ModuleDescriptor<?> getPluginModule(final ModuleCompleteKey key) {
        final Plugin plugin = getPlugin(key.getPluginKey());
        if (plugin == null) {
            return null;
        }
        return plugin.getModuleDescriptor(key.getModuleKey());
    }

    public ModuleDescriptor<?> getEnabledPluginModule(final String completeKey) {
        final ModuleCompleteKey key = new ModuleCompleteKey(completeKey);

        // If it's disabled, return null
        if (!isPluginModuleEnabled(key)) {
            return null;
        }

        return getEnabledPlugin(key.getPluginKey()).getModuleDescriptor(key.getModuleKey());
    }

    /**
     * @see PluginAccessor#getEnabledModulesByClass(Class)
     */
    public <M> List<M> getEnabledModulesByClass(final Class<M> moduleClass) {
        return getModules(getEnabledModuleDescriptorsByModuleClass(moduleClass));
    }

    /**
     * @see PluginAccessor#getEnabledModulesByClassAndDescriptor(Class[], Class)
     * @deprecated since 0.17, use
     *             {@link #getModules(org.maera.plugin.predicate.ModuleDescriptorPredicate)}
     *             with an appropriate predicate instead.
     */
    @Deprecated
    public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>>[] descriptorClasses, final Class<M> moduleClass) {
        final Iterable<ModuleDescriptor<M>> moduleDescriptors = filterDescriptors(getEnabledModuleDescriptorsByModuleClass(moduleClass), new ModuleDescriptorOfClassPredicate<M>(
                descriptorClasses));

        return getModules(moduleDescriptors);
    }

    /**
     * @see PluginAccessor#getEnabledModulesByClassAndDescriptor(Class, Class)
     * @deprecated since 0.17, use
     *             {@link #getModules(org.maera.plugin.predicate.ModuleDescriptorPredicate)}
     *             with an appropriate predicate instead.
     */
    @Deprecated
    public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>> descriptorClass, final Class<M> moduleClass) {
        final Iterable<ModuleDescriptor<M>> moduleDescriptors = getEnabledModuleDescriptorsByModuleClass(moduleClass);
        return getModules(filterDescriptors(moduleDescriptors, new ModuleDescriptorOfClassPredicate<M>(descriptorClass)));
    }

    /**
     * Get all module descriptor that are enabled and for which the module is an
     * instance of the given class.
     *
     * @param moduleClass the class of the module within the module descriptor.
     * @return a collection of {@link ModuleDescriptor}s
     */
    private <M> Collection<ModuleDescriptor<M>> getEnabledModuleDescriptorsByModuleClass(final Class<M> moduleClass) {
        final ModuleOfClassPredicate<M> ofType = new ModuleOfClassPredicate<M>(moduleClass);
        final EnabledModulePredicate<M> enabled = new EnabledModulePredicate<M>(this);
        return toList(getModuleDescriptors(getEnabledPlugins(), new ModuleDescriptorPredicate<M>() {
            public boolean matches(final ModuleDescriptor<? extends M> moduleDescriptor) {
                return ofType.matches(moduleDescriptor) && enabled.matches(moduleDescriptor);
            }
        }));
    }

    /**
     * This method has been reverted to pre PLUG-40 to fix performance issues
     * that were encountered during load testing. This should be reverted to the
     * state it was in at 54639 when the fundamental issue leading to this
     * slowdown has been corrected (that is, slowness of PluginClassLoader).
     *
     * @see PluginAccessor#getEnabledModuleDescriptorsByClass(Class)
     */
    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz) {
        final List<D> result = new LinkedList<D>();
        for (final Plugin plugin : plugins.values()) {
            // Skip disabled plugins
            if (!isPluginEnabled(plugin.getKey())) {
                if (log.isDebugEnabled()) {
                    log.debug("Plugin [" + plugin.getKey() + "] is disabled.");
                }
                continue;
            }

            for (final ModuleDescriptor<?> module : plugin.getModuleDescriptors()) {
                if (descriptorClazz.isInstance(module)) {
                    if (isPluginModuleEnabled(module.getCompleteKey())) {
                        @SuppressWarnings("unchecked")
                        final D moduleDescriptor = (D) module;
                        result.add(moduleDescriptor);
                    } else if (log.isDebugEnabled()) {
                        log.debug("Module [" + module.getCompleteKey() + "] is disabled.");
                    }
                }
            }
        }

        return result;
    }

    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz, final boolean verbose) {
        return getEnabledModuleDescriptorsByClass(descriptorClazz);
    }

    /**
     * @see PluginAccessor#getEnabledModuleDescriptorsByType(String)
     * @deprecated since 0.17, use
     *             {@link #getModuleDescriptors(org.maera.plugin.predicate.ModuleDescriptorPredicate)}
     *             with an appropriate predicate instead.
     */
    @Deprecated
    public <M> List<ModuleDescriptor<M>> getEnabledModuleDescriptorsByType(final String type) throws PluginParseException, IllegalArgumentException {
        final ModuleDescriptorOfTypePredicate<M> ofType = new ModuleDescriptorOfTypePredicate<M>(moduleDescriptorFactory, type);
        final EnabledModulePredicate<M> enabled = new EnabledModulePredicate<M>(this);
        return toList(getModuleDescriptors(getEnabledPlugins(), new ModuleDescriptorPredicate<M>() {
            public boolean matches(final ModuleDescriptor<? extends M> moduleDescriptor) {
                return ofType.matches(moduleDescriptor) && enabled.matches(moduleDescriptor);
            }
        }));
    }

    /**
     * Filters out a collection of {@link ModuleDescriptor}s given a predicate.
     *
     * @param descriptors the collection of {@link ModuleDescriptor}s to filter.
     * @param predicate   the predicate to use for filtering.
     */
    private static <M> Iterable<ModuleDescriptor<M>> filterDescriptors(final Iterable<ModuleDescriptor<M>> descriptors, final ModuleDescriptorPredicate<M> predicate) {
        return filter(descriptors, new Predicate<ModuleDescriptor<M>>() {
            public boolean apply(final ModuleDescriptor<M> input) {
                return predicate.matches(input);
            }
        });
    }

    /**
     * Enable a set of plugins by key. This will implicitly and recursively
     * enable all dependent plugins.
     *
     * @param keys The plugin keys. Must not be null.
     * @since 2.5.0
     */
    public void enablePlugins(final String... keys) {
        final Collection<Plugin> pluginsToEnable = new ArrayList<Plugin>(keys.length);

        for (final String key : keys) {
            if (key == null) {
                throw new IllegalArgumentException("Keys passed to enablePlugins must be non-null");
            }

            final Plugin plugin = plugins.get(key);
            if (plugin == null) {
                if (log.isInfoEnabled()) {
                    log.info("No plugin was found for key '" + key + "'. Not enabling.");
                }
                continue;
            }

            if (!plugin.getPluginInformation().satisfiesMinJavaVersion()) {
                log.error("Minimum Java version of '" + plugin.getPluginInformation().getMinJavaVersion() + "' was not satisfied for module '" + key + "'. Not enabling.");
                continue;
            }
            pluginsToEnable.add(plugin);
        }
        final Collection<Plugin> enabledPlugins = pluginEnabler.enableAllRecursively(pluginsToEnable);

        for (final Plugin plugin : enabledPlugins) {
            enablePluginState(plugin, getStore());
            notifyPluginEnabled(plugin);
        }
    }

    /**
     * @deprecated since 2.5.0, use {#link enablePlugins(String... keys)} instead
     */
    @Deprecated
    public void enablePlugin(final String key) {
        enablePlugins(key);
    }

    protected void enablePluginState(final Plugin plugin, final PluginPersistentStateStore stateStore) {
        stateStore.save(getBuilder().setEnabled(plugin, true).toState());
    }

    /**
     * Called on all clustered application nodes, rather than
     * {@link #enablePlugin(String)} to just update the local state, state aware
     * modules and loaders, but not affect the global plugin state.
     *
     * @param plugin the plugin being enabled
     */
    protected void notifyPluginEnabled(final Plugin plugin) {
        plugin.enable();
        if (enableConfiguredPluginModules(plugin)) {
            pluginEventManager.broadcast(new PluginEnabledEvent(plugin));
        }
    }

    /**
     * For each module in the plugin, call the module descriptor's enabled()
     * method if the module is StateAware and enabled.
     * <p/>
     * If any modules fail to enable then the plugin is replaced by an
     * UnloadablePlugin, and this method will return {@code false}.
     *
     * @param plugin the plugin to enable
     * @return true if the modules were all enabled correctly, false otherwise.
     */
    private boolean enableConfiguredPluginModules(final Plugin plugin) {
        boolean success = true;
        final Set<ModuleDescriptor<?>> enabledDescriptors = new HashSet<ModuleDescriptor<?>>();
        for (final ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors()) {
            if (!enableConfiguredPluginModule(plugin, descriptor, enabledDescriptors)) {
                success = false;
                break;
            }
        }
        return success;
    }

    private boolean enableConfiguredPluginModule(Plugin plugin, ModuleDescriptor<?> descriptor, Set<ModuleDescriptor<?>> enabledDescriptors) {
        boolean success = true;

        // This can happen if the plugin available event is fired as part of the plugin initialization process
        if (!isPluginEnabled(plugin.getKey())) {
            log.debug("The plugin isn't enabled, so we won't bother trying to enable it");
            return success;
        }

        // We only want to re-enable modules that weren't explicitly
        // disabled by the user.
        if (!isPluginModuleEnabled(descriptor.getCompleteKey())) {
            if (plugin.isSystemPlugin()) {
                log.warn("System plugin module disabled: " + descriptor.getCompleteKey());
            } else if (log.isDebugEnabled()) {
                log.debug("Plugin module '" + descriptor.getName() + "' is explicitly disabled, so not re-enabling.");
            }
            return success;
        }

        try {
            notifyModuleEnabled(descriptor);
            enabledDescriptors.add(descriptor);
        }
        catch (final Throwable exception) {
            // catch any errors and insert an UnloadablePlugin (PLUG-7)
            log.error("There was an error loading the descriptor '" + descriptor.getName() + "' of plugin '" + plugin.getKey() + "'. Disabling.", exception);

            // Disable all previously enabled descriptors
            for (final ModuleDescriptor<?> desc : enabledDescriptors) {
                notifyModuleDisabled(desc);
            }

            replacePluginWithUnloadablePlugin(plugin, descriptor, exception);
            success = false;
        }

        return success;
    }

    public void disablePlugin(final String key) {
        disablePluginInternal(key, true);
    }

    public void disablePluginWithoutPersisting(final String key) {
        disablePluginInternal(key, false);
    }

    protected void disablePluginInternal(final String key, final boolean persistDisabledState) {
        if (key == null) {
            throw new IllegalArgumentException("You must specify a plugin key to disable.");
        }

        final Plugin plugin = plugins.get(key);
        if (plugin == null) {
            if (log.isInfoEnabled()) {
                log.info("No plugin was found for key '" + key + "'. Not disabling.");
            }
            return;
        }

        notifyPluginDisabled(plugin);
        if (persistDisabledState) {
            disablePluginState(plugin, getStore());
        }
    }

    protected void disablePluginState(final Plugin plugin, final PluginPersistentStateStore stateStore) {
        stateStore.save(getBuilder().setEnabled(plugin, false).toState());
    }

    protected void notifyPluginDisabled(final Plugin plugin) {
        log.info("Disabling " + plugin.getKey());
        disablePluginModules(plugin);

        // This needs to happen after modules are disabled to prevent errors
        plugin.disable();
        pluginEventManager.broadcast(new PluginDisabledEvent(plugin));
    }

    private void disablePluginModules(final Plugin plugin) {
        final List<ModuleDescriptor<?>> moduleDescriptors = new ArrayList<ModuleDescriptor<?>>(plugin.getModuleDescriptors());
        Collections.reverse(moduleDescriptors); // disable in reverse order

        for (final ModuleDescriptor<?> module : moduleDescriptors) {
            // don't actually disable the module, just fire the events because
            // its plugin is being disabled
            // if the module was actually disabled, you'd have to reenable each
            // one when enabling the plugin

            if (isPluginModuleEnabled(module.getCompleteKey())) {
                publishModuleDisabledEvents(module);
            }
        }
    }

    public void disablePluginModule(final String completeKey) {
        if (completeKey == null) {
            throw new IllegalArgumentException("You must specify a plugin module key to disable.");
        }

        final ModuleDescriptor<?> module = getPluginModule(completeKey);

        if (module == null) {
            if (log.isInfoEnabled()) {
                log.info("Returned module for key '" + completeKey + "' was null. Not disabling.");
            }
            return;
        }
        if (module.getClass().isAnnotationPresent(CannotDisable.class)) {
            if (log.isInfoEnabled()) {
                log.info("Plugin module " + completeKey + " cannot be disabled; it is annotated with" + CannotDisable.class.getName());
            }
            return;
        }

        disablePluginModuleState(module, getStore());
        notifyModuleDisabled(module);
    }

    protected void disablePluginModuleState(final ModuleDescriptor<?> module, final PluginPersistentStateStore stateStore) {
        stateStore.save(getBuilder().setEnabled(module, false).toState());
    }

    protected void notifyModuleDisabled(final ModuleDescriptor<?> module) {
        publishModuleDisabledEvents(module);
    }

    private void publishModuleDisabledEvents(final ModuleDescriptor<?> module) {
        if (log.isDebugEnabled()) {
            log.debug("Disabling " + module.getKey());
        }

        if (module instanceof StateAware) {
            ((StateAware) module).disabled();
        }

        pluginEventManager.broadcast(new PluginModuleDisabledEvent(module));
    }

    public void enablePluginModule(final String completeKey) {
        if (completeKey == null) {
            throw new IllegalArgumentException("You must specify a plugin module key to disable.");
        }

        final ModuleDescriptor<?> module = getPluginModule(completeKey);

        if (module == null) {
            if (log.isInfoEnabled()) {
                log.info("Returned module for key '" + completeKey + "' was null. Not enabling.");
            }

            return;
        }

        if (!module.satisfiesMinJavaVersion()) {
            log.error("Minimum Java version of '" + module.getMinJavaVersion() + "' was not satisfied for module '" + completeKey + "'. Not enabling.");
            return;
        }
        enablePluginModuleState(module, getStore());
        notifyModuleEnabled(module);
    }

    protected void enablePluginModuleState(final ModuleDescriptor<?> module, final PluginPersistentStateStore stateStore) {
        stateStore.save(getBuilder().setEnabled(module, true).toState());
    }

    protected void notifyModuleEnabled(final ModuleDescriptor<?> module) {
        if (log.isDebugEnabled()) {
            log.debug("Enabling " + module.getKey());
        }
        if (module instanceof StateAware) {
            ((StateAware) module).enabled();
        }
        pluginEventManager.broadcast(new PluginModuleEnabledEvent(module));
    }

    public boolean isPluginModuleEnabled(final String completeKey) {
        // completeKey may be null
        return (completeKey == null) ? false : isPluginModuleEnabled(new ModuleCompleteKey(completeKey));
    }

    private boolean isPluginModuleEnabled(final ModuleCompleteKey key) {
        if (!isPluginEnabled(key.getPluginKey())) {
            return false;
        }
        final ModuleDescriptor<?> pluginModule = getPluginModule(key);
        return (pluginModule != null) && getState().isEnabled(pluginModule);
    }

    /**
     * This method checks to see if the plugin is enabled based on the state
     * manager and the plugin.
     *
     * @param key The plugin key
     * @return True if the plugin is enabled
     */
    public boolean isPluginEnabled(final String key) {
        final Plugin plugin = plugins.get(notNull("The plugin key must be specified", key));

        return (plugin != null) && ((plugin.getPluginState() == PluginState.ENABLED) && getState().isEnabled(plugin));
    }

    public InputStream getDynamicResourceAsStream(final String name) {
        return getClassLoader().getResourceAsStream(name);
    }

    public Class<?> getDynamicPluginClass(final String className) throws ClassNotFoundException {
        return getClassLoader().loadClass(className);
    }

    public PluginsClassLoader getClassLoader() {
        return classLoader;
    }

    public InputStream getPluginResourceAsStream(final String pluginKey, final String resourcePath) {
        final Plugin plugin = getEnabledPlugin(pluginKey);
        if (plugin == null) {
            log.error("Attempted to retreive resource " + resourcePath + " for non-existent or inactive plugin " + pluginKey);
            return null;
        }

        return plugin.getResourceAsStream(resourcePath);
    }

    /**
     * Disables and replaces a plugin currently loaded with an UnloadablePlugin.
     *
     * @param plugin     the plugin to be replaced
     * @param descriptor the descriptor which caused the problem
     * @param throwable  the problem caught when enabling the descriptor
     * @return the UnloadablePlugin which replaced the broken plugin
     */
    private UnloadablePlugin replacePluginWithUnloadablePlugin(final Plugin plugin, final ModuleDescriptor<?> descriptor, final Throwable throwable) {
        final UnloadableModuleDescriptor unloadableDescriptor = UnloadableModuleDescriptorFactory.createUnloadableModuleDescriptor(plugin, descriptor, throwable);
        final UnloadablePlugin unloadablePlugin = UnloadablePluginFactory.createUnloadablePlugin(plugin, unloadableDescriptor);

        unloadablePlugin.setUninstallable(plugin.isUninstallable());
        unloadablePlugin.setDeletable(plugin.isDeleteable());
        // Add the error text at the plugin level as well. This is useful for
        // logging.
        unloadablePlugin.setErrorText(unloadableDescriptor.getErrorText());
        plugins.put(plugin.getKey(), unloadablePlugin);

        // PLUG-390: We used to persist the disabled state here, but we don't
        // want to do this.
        // We want to try load this plugin again on restart as the user may have
        // installed a fixed version of this plugin.
        return unloadablePlugin;
    }

    public boolean isSystemPlugin(final String key) {
        final Plugin plugin = getPlugin(key);
        return (plugin != null) && plugin.isSystemPlugin();
    }

    public PluginRestartState getPluginRestartState(final String key) {
        return getState().getPluginRestartState(key);
    }

    private Builder getBuilder() {
        return PluginPersistentState.Builder.create(getStore().load());
    }

    /**
     * @deprecated Since 2.0.0.beta2
     */
    @Deprecated
    public void setDescriptorParserFactory(final DescriptorParserFactory descriptorParserFactory) {
    }
}
