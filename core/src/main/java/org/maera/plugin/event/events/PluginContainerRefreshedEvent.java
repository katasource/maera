package org.maera.plugin.event.events;

import org.apache.commons.lang.Validate;

/**
 * Event for when the container a plugin is installed into has been refreshed
 *
 * @since 2.2.0
 */
public class PluginContainerRefreshedEvent {
    private final Object container;
    private final String key;

    public PluginContainerRefreshedEvent(Object container, String key) {
        Validate.notNull(key, "The plugin key must be available");
        Validate.notNull(container, "The container cannot be null");

        this.container = container;
        this.key = key;
    }

    public Object getContainer() {
        return container;
    }

    public String getPluginKey() {
        return key;
    }
}
