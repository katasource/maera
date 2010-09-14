package org.maera.plugin;

/**
 * Makes a plugin or plugin module aware of its activation state. Plugins or modules should implement this
 * interface if they want to be notified when they are enabled and disabled.
 */
public interface StateAware {
    /**
     * Called by the plugin manager when a plugin or module is activated. Any exceptions thrown should
     * be interpreted as the plugin is unloadable.
     */
    void enabled();

    /**
     * Called by the plugin manager when the plugin or module is deactivated. This method will only
     * be called if the plugin is deactivated while the application is running: stopping
     * the server will <i>not</i> cause this method to be called on any plugins.
     */
    void disabled();
}
