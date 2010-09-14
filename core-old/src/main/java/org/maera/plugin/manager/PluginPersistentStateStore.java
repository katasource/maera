package org.maera.plugin.manager;

/**
 * A wrapper object that the user implements to store the persisted state of plugins.
 *
 * @since 2.2.0
 */
public interface PluginPersistentStateStore {
    /**
     * Put the current global state of plugin activation/deactivation into permanent
     * storage. The map passed in should have keys of the form "plugin" or
     * "plugin:module", and Boolean values representing whether the plugin or
     * module is enabled (true if it's enabled).
     * <p/>
     * <p>Callers should only pass in values for those plugins or modules that are
     * <i>not</i> in their default state.
     *
     * @param state the map of plugin and module activation states
     */
    void save(PluginPersistentState state);

    /**
     * Get the saved activation state of loaded plugins or modules. The map
     * will be identical to the one described in savePluginState.
     *
     * @return the configured activation/deactivation state for plugins in this Confluence
     *         instance.
     */
    PluginPersistentState load();
}

