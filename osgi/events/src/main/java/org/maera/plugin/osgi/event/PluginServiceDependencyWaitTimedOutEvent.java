package org.maera.plugin.osgi.event;

import org.osgi.framework.Filter;

/**
 * Event for when a plugin has timed out waiting for an OSGi service to be available
 *
 * @since 2.2.1
 */
public class PluginServiceDependencyWaitTimedOutEvent extends AbstractPluginServiceDependencyWaitEvent {
    private final long elapsedTime;

    public PluginServiceDependencyWaitTimedOutEvent(String pluginKey, String beanName, Filter filter, long elapsedTime) {
        super(pluginKey, beanName, filter);
        this.elapsedTime = elapsedTime;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }
}