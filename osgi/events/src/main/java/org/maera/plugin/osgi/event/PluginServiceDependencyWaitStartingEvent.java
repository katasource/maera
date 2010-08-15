package org.maera.plugin.osgi.event;

import org.osgi.framework.Filter;

/**
 * Event for when a plugin has started to wait for an OSGi service to be available.
 *
 * @since 2.2.1
 */
public class PluginServiceDependencyWaitStartingEvent extends AbstractPluginServiceDependencyWaitEvent {
    private final long waitTime;

    public PluginServiceDependencyWaitStartingEvent(String pluginKey, String beanName, Filter filter, long waitTime) {
        super(pluginKey, beanName, filter);
        this.waitTime = waitTime;
    }

    public long getWaitTime() {
        return waitTime;
    }
}
