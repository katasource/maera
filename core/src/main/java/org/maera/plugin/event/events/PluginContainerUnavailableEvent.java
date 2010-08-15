package org.maera.plugin.event.events;

import org.apache.commons.lang.Validate;

/**
 * Event for when the container of a plugin is been shutdown, usually as a result of the OSGi bundle being stopped
 *
 * @since 2.5.0
 */
public class PluginContainerUnavailableEvent {
    private final String key;

    public PluginContainerUnavailableEvent(String key) {
        Validate.notNull(key, "The plugin key must be available");

        this.key = key;
    }

    public String getPluginKey() {
        return key;
    }
}