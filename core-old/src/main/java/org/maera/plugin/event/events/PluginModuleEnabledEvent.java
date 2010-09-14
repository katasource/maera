package org.maera.plugin.event.events;

import org.maera.plugin.ModuleDescriptor;

/**
 * Event fired when a plugin module is enabled, which can also happen when its
 * plugin is enabled or installed.
 *
 * @see org.maera.plugin.event.events
 */
public class PluginModuleEnabledEvent {
    private final ModuleDescriptor module;

    public PluginModuleEnabledEvent(ModuleDescriptor module) {
        this.module = module;
    }

    public ModuleDescriptor getModule() {
        return module;
    }
}
