package org.maera.plugin.osgi.event;

import org.osgi.framework.Filter;

/**
 * Events that are fired when OSGi services are waiting to be resolved.
 *
 * @since 2.2.0
 */
public interface PluginServiceDependencyWaitEvent {
    /**
     * @return the filter used for the resolution.  May be null.
     */
    Filter getFilter();

    /**
     * @return the Spring bean name for the service reference.  May be null.
     */
    String getBeanName();

    /**
     * @return the key for the plugin waiting for the dependency.  May be null if unknown.
     */
    String getPluginKey();
}
