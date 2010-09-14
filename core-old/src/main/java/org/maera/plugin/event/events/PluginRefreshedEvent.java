package org.maera.plugin.event.events;

import org.maera.plugin.Plugin;

/**
 * Event fired when the plugin has been refreshed with no user interaction
 *
 * @since 2.2.0
 */
public class PluginRefreshedEvent {
    private final Plugin plugin;

    public PluginRefreshedEvent(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
