package net.maera.plugin;

/**
 * Enumeration representing the possible states a Plugin may be in during runtime.
 *
 * @since 0.1
 */
public enum PluginState {

    /**
     * The plugin has been installed into the plugin system
     */
    INSTALLED,

    /**
     * The plugin is in the process of being enabled
     */
    ENABLING,

    /**
     * The plugin has been enabled
     */
    ENABLED,

    /**
     * The plugin is in the process of being disabled.
     */
    DISABLING,

    /**
     * The plugin has been disabled
     */
    DISABLED,

    /**
     * The plugin has been uninstalled and should be unavailable
     */
    UNINSTALLED
}
