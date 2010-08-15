package org.maera.plugin.event.events;

import org.maera.plugin.Plugin;

/**
 * Event fired when a plugin is explicited uninstalled (as opposed to as part of an upgrade).
 *
 * @see org.maera.plugin.event.events
 * @since 2.5
 */
public class PluginUninstalledEvent {
    private final Plugin plugin;

    public PluginUninstalledEvent(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}
