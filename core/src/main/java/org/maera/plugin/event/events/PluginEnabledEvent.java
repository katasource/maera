package org.maera.plugin.event.events;

import org.maera.plugin.Plugin;

/**
 * Event fired when a plugin is enabled, installed or updated.
 *
 * @see org.maera.plugin.event.events
 */
public class PluginEnabledEvent {
    private final Plugin plugin;

    public PluginEnabledEvent(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
