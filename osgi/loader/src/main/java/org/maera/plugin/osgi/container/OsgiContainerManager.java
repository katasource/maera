package org.maera.plugin.osgi.container;

import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.util.List;

/**
 * Manages the OSGi container and handles any interactions with it
 */
public interface OsgiContainerManager {
    /**
     * Starts the OSGi container
     *
     * @throws OsgiContainerException If the container cannot be started
     */
    void start() throws OsgiContainerException;

    /**
     * Stops the OSGi container
     *
     * @throws OsgiContainerException If the container cannot be stopped
     */
    void stop() throws OsgiContainerException;

    /**
     * Installs a bundle into a running OSGI container
     *
     * @param file The bundle file to install
     * @return The created bundle
     * @throws OsgiContainerException If the bundle cannot be loaded
     */
    Bundle installBundle(File file) throws OsgiContainerException;

    /**
     * @return If the container is running or not
     */
    boolean isRunning();

    /**
     * Gets a list of installed bundles
     *
     * @return An array of bundles
     */
    Bundle[] getBundles();

    /**
     * Gets a list of service references
     *
     * @return An array of service references
     */
    ServiceReference[] getRegisteredServices();

    /**
     * Gets a list of host component registrations
     *
     * @return A list of host component registrations
     */
    List<HostComponentRegistration> getHostComponentRegistrations();

    /**
     * Gets a service tracker to follow a service registered under a certain interface.  Will return a new
     * {@link ServiceTracker} instance for every call, so don't call more than necessary.  Any provided
     * {@link ServiceTracker} instances will be opened before returning and automatically closed on shutdown.
     *
     * @param interfaceClassName The interface class as a String
     * @return A service tracker to follow all instances of that interface
     * @throws IllegalStateException If the OSGi container is not running
     * @since 2.1
     */
    ServiceTracker getServiceTracker(String interfaceClassName);
}
