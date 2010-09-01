package org.maera.plugin.osgi.container;

/**
 * Event fired when the OSGi container has started
 *
 * @since 0.1
 */
public class OsgiContainerStartedEvent {

    private final OsgiContainerManager osgiContainerManager;

    public OsgiContainerStartedEvent(OsgiContainerManager osgiContainerManager) {
        this.osgiContainerManager = osgiContainerManager;
    }

    public OsgiContainerManager getOsgiContainerManager() {
        return osgiContainerManager;
    }
}
