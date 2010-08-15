package org.maera.plugin.event.events;

import org.maera.plugin.Plugin;

/**
 * Event that signifies a plugin has been disabled, uninstalled or updated.
 *
 * @see org.maera.plugin.event.events
 */
public class PluginDisabledEvent {
    private final Plugin plugin;

    public PluginDisabledEvent(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
