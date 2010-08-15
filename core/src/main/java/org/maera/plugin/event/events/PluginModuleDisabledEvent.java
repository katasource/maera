package org.maera.plugin.event.events;

import org.maera.plugin.ModuleDescriptor;

/**
 * Event fired when a plugin module is disabled, which can also happen when its
 * plugin is disabled or uninstalled.
 *
 * @see org.maera.plugin.event.events
 */
public class PluginModuleDisabledEvent {
    private final ModuleDescriptor module;

    public PluginModuleDisabledEvent(ModuleDescriptor module) {
        this.module = module;
    }

    public ModuleDescriptor getModule() {
        return module;
    }
}