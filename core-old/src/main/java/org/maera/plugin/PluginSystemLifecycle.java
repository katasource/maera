package org.maera.plugin;

import org.maera.plugin.event.NotificationException;

/**
 * Controls the life-cycle of the plugin system.
 *
 * @since 2.2.0
 */
public interface PluginSystemLifecycle {
    /**
     * Initialise the plugin system. This <b>must</b> be called before anything else.
     *
     * @throws PluginParseException  If parsing the plugins failed.
     * @throws IllegalStateException if already initialized or already in the process of initialization.
     * @throws NotificationException If any of the Event Listeners throw an exception on the Framework startup events.
     */
    void init() throws PluginParseException, NotificationException;

    /**
     * Destroys the plugin manager. This <b>must</b> be called when getting rid of the manager instance and you
     * plan to create another one. Failure to do so will leave around significant resources including threads
     * and memory usage and can interfere with a web-application being correctly shutdown.
     *
     * @since 2.0.0
     */
    void shutdown();

    /**
     * Restart all plugins by disabling and enabling them in the order they were loaded (by plugin loader)
     *
     * @since 2.3.0
     */
    void warmRestart();
}
