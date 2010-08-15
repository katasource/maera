package org.maera.plugin.event.events;

import org.apache.commons.lang.Validate;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.PluginController;

/**
 * Event that signifies the plugin framework is being started
 */
public class PluginFrameworkStartingEvent {
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;

    public PluginFrameworkStartingEvent(PluginController pluginController, PluginAccessor pluginAccessor) {
        Validate.notNull(pluginController);
        Validate.notNull(pluginAccessor);
        this.pluginController = pluginController;
        this.pluginAccessor = pluginAccessor;
    }

    public PluginController getPluginController() {
        return pluginController;
    }

    public PluginAccessor getPluginAccessor() {
        return pluginAccessor;
    }
}