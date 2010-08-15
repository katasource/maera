package org.maera.plugin.osgi.factory;

import org.maera.plugin.AutowireCapablePlugin;
import org.maera.plugin.IllegalPluginStateException;
import org.maera.plugin.module.ContainerAccessor;
import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;

import java.io.InputStream;
import java.net.URL;
import java.util.Set;

/**
 * Helper for the {@link OsgiPlugin} to abstract how key operations are handled in different states, represented
 * by implementations of this interface.
 *
 * @since 2.2.0
 */
interface OsgiPluginHelper {

    /**
     * @return the OSGi bundle
     */
    Bundle getBundle();

    /**
     * Loads a class from the bundle
     *
     * @param clazz        The class name to load
     * @param callingClass The calling class
     * @param <T>          The type of class to load
     * @return An instance of the class
     * @throws ClassNotFoundException If the class cannot be found
     */
    <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException;

    /**
     * Gets a resource from the bundle
     *
     * @param name The resource name
     * @return The resource
     */
    URL getResource(final String name);

    /**
     * Gets a resource as a stream from the bundle
     *
     * @param name The resource name
     * @return The input stream
     */
    InputStream getResourceAsStream(final String name);

    /**
     * Gets the classloader for this bundle
     *
     * @return The class loader instance
     */
    ClassLoader getClassLoader();

    /**
     * Installs the bundle
     *
     * @return The created bundle
     */
    Bundle install();

    /**
     * Notification the bundle has been enabled
     *
     * @param serviceTrackers The service trackers to associate with the bundle
     */
    void onEnable(ServiceTracker... serviceTrackers);

    /**
     * Notification that the plugin has been disabled
     */
    void onDisable();

    /**
     * Notification the bundle has been uninstalled
     */
    void onUninstall();

    /**
     * If spring is required, it looks for the spring application context, and calls createBean().  If not, the class
     * is instantiated with its default constructor.
     *
     * @param clazz            The class to autowire The class to create
     * @param autowireStrategy The autowire strategy to use The strategy to use, only respected if spring is available
     * @param <T>              The class type
     * @return The autowired instance
     * @throws IllegalPluginStateException If spring is required but not available
     */
    <T> T autowire(final Class<T> clazz, final AutowireCapablePlugin.AutowireStrategy autowireStrategy) throws IllegalPluginStateException;

    /**
     * Autowires a class instance
     *
     * @param instance         The instance to autowire
     * @param autowireStrategy The autowire strategy to use
     * @throws IllegalStateException If autowiring is not available
     */
    void autowire(final Object instance, final AutowireCapablePlugin.AutowireStrategy autowireStrategy) throws IllegalStateException;

    /**
     * @return a list of required plugins
     */
    Set<String> getRequiredPlugins();

    /**
     * @param container the plugin container (spring context)
     */
    void setPluginContainer(Object container);

    ContainerAccessor getContainerAccessor();
}
