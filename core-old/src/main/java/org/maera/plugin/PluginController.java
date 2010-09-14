package org.maera.plugin;

import java.util.Set;

/**
 * Interface to control the state of the plugin system
 */
public interface PluginController {
    /**
     * Enable a plugin by key.
     *
     * @param key The plugin key.
     * @deprecated since 2.5.0, use {#link enablePlugins(String... keys)} instead
     */
    void enablePlugin(String key);

    /**
     * Enable a set of plugins by key. This will implicitly and recursively enable all dependent plugins
     *
     * @param keys The plugin keys. Must not be null.
     * @since 2.5.0
     */
    void enablePlugins(String... keys);

    /**
     * Disables the plugin with the given key.
     * <p/>
     * <p>Calling this method will persist the disabled state so that the plugin will also be disabled on next startup.
     * This would normally be used when a user manually disables a plugin.
     * <p/>
     * <p>If you extend DefaultPluginManager and override this method, you will also need to override {@link #disablePluginWithoutPersisting(String)}.
     *
     * @param key The plugin key.
     * @see #disablePluginWithoutPersisting(String)
     */
    void disablePlugin(String key);

    /**
     * Disables the plugin with the given key without persisting the disabled state.
     * <p/>
     * <p>Calling this method will NOT persist the disabled state so that the framework will try to enable the plugin on next startup.
     * This is used when a plugin has errors on startup.
     * <p/>
     * <p>If you extend DefaultPluginManager and override {@link #disablePlugin(String)}, you will also need to override this method.
     *
     * @param key The plugin key.
     * @see #disablePlugin(String)
     * @since 2.3.0
     */
    void disablePluginWithoutPersisting(String key);

    /**
     * Enable a plugin module by key.
     *
     * @param completeKey The "complete key" of the plugin module.
     */
    void enablePluginModule(String completeKey);

    /**
     * Disable a plugin module by key.
     *
     * @param completeKey The "complete key" of the plugin module.
     */
    void disablePluginModule(String completeKey);

    /**
     * Installs a plugin and returns the plugin key
     *
     * @param pluginArtifact The plugin artifact to install
     * @return The plugin key
     * @throws org.maera.plugin.PluginParseException
     *          if the plugin is not a valid plugin
     * @deprecated Since 2.3.0, use {@link #installPlugins(PluginArtifact...)} instead
     */
    String installPlugin(PluginArtifact pluginArtifact) throws PluginParseException;

    /**
     * Installs multiple plugins and returns the list of plugin keys.  All plugin artifacts must be for valid plugins
     * or none will be installed.
     *
     * @param pluginArtifacts The list of plugin artifacts to install
     * @return A list of plugin keys
     * @throws org.maera.plugin.PluginParseException
     *          if any plugin is not a valid plugin
     * @since 2.3.0
     */
    Set<String> installPlugins(PluginArtifact... pluginArtifacts) throws PluginParseException;

    /**
     * Uninstall the plugin, disabling it first.
     *
     * @param plugin The plugin.
     * @throws PluginException if there was some problem uninstalling the plugin.
     */
    void uninstall(Plugin plugin) throws PluginException;

    /**
     * Restores the state of any plugin requiring a restart that had been removed, upgraded, or installed.  If marked
     * as removed, the mark will be deleted.  If marked as upgrade, an attempt to restore the original plugin artifact
     * will be made.  If marked as install, the artifact will be deleted.
     *
     * @param pluginKey The plugin key
     * @throws PluginException          if there was some problem reverting the plugin state.
     * @throws IllegalArgumentException if the plugin key is null or cannot be resolved to a plugin
     * @since 2.5.0
     */
    void revertRestartRequiredChange(String pluginKey) throws PluginException;

    /**
     * Search all loaders and add any new plugins you find.
     *
     * @return The number of new plugins found.
     */
    int scanForNewPlugins() throws PluginParseException;
}
