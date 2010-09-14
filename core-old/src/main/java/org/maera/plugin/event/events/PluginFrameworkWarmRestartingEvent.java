package org.maera.plugin.event.events;

import org.apache.commons.lang.Validate;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.PluginController;

/**
 * Signals a warm restart of the plugin framework is about to begin
 *
 * @since 2.3.0
 */
public class PluginFrameworkWarmRestartingEvent {
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;

    public PluginFrameworkWarmRestartingEvent(PluginController pluginController, PluginAccessor pluginAccessor) {
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
