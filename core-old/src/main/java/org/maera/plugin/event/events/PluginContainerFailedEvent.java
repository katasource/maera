package org.maera.plugin.event.events;

import org.apache.commons.lang.Validate;

/**
 * Event thrown when the container a plugin is installed into either rejects the plugin or fails altogether
 *
 * @since 2.2.0
 */
public class PluginContainerFailedEvent {
    private final Object container;
    private final String key;
    private final Throwable cause;

    public PluginContainerFailedEvent(Object container, String key, Throwable cause) {
        Validate.notNull(key, "The bundle symbolic name must be available");

        this.container = container;
        this.key = key;
        this.cause = cause;
    }

    public Object getContainer() {
        return container;
    }

    public String getPluginKey() {
        return key;
    }

    public Throwable getCause() {
        return cause;
    }
}
