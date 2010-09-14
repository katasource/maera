package org.maera.plugin.event.events;

import org.apache.commons.lang.Validate;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.PluginController;

/**
 * Signals a warm restart of the plugin framework has been completed
 *
 * @since 2.3.0
 */
public class PluginFrameworkWarmRestartedEvent {
    private final PluginController pluginController;
    private final PluginAccessor pluginAccessor;

    public PluginFrameworkWarmRestartedEvent(PluginController pluginController, PluginAccessor pluginAccessor) {
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