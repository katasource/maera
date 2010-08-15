package org.maera.plugin.osgi.container;

/**
 * Event fired when the OSGi container has stopped
 *
 * @since 2.5.0
 */
public class OsgiContainerStoppedEvent {
    private final OsgiContainerManager osgiContainerManager;

    public OsgiContainerStoppedEvent(OsgiContainerManager osgiContainerManager) {
        this.osgiContainerManager = osgiContainerManager;
    }

    public OsgiContainerManager getOsgiContainerManager() {
        return osgiContainerManager;
    }
}