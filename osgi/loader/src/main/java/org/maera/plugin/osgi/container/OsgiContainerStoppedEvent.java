package org.maera.plugin.osgi.container;

/**
 * Event fired when the OSGi container has stopped
 *
 * @since 0.1
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