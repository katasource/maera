package org.maera.plugin.osgi.factory;

import org.apache.commons.lang.Validate;
import org.dom4j.Element;
import org.maera.plugin.AutowireCapablePlugin;
import org.maera.plugin.IllegalPluginStateException;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginState;
import org.maera.plugin.event.PluginEventListener;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginContainerFailedEvent;
import org.maera.plugin.event.events.PluginContainerRefreshedEvent;
import org.maera.plugin.event.events.PluginRefreshedEvent;
import org.maera.plugin.impl.AbstractPlugin;
import org.maera.plugin.module.ContainerAccessor;
import org.maera.plugin.module.ContainerManagedPlugin;
import org.maera.plugin.osgi.container.OsgiContainerException;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.event.PluginServiceDependencyWaitEndedEvent;
import org.maera.plugin.osgi.event.PluginServiceDependencyWaitStartingEvent;
import org.maera.plugin.osgi.event.PluginServiceDependencyWaitTimedOutEvent;
import org.maera.plugin.osgi.external.ListableModuleDescriptorFactory;
import org.maera.plugin.util.PluginUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Plugin that wraps an OSGi bundle that does contain a plugin descriptor.  The actual bundle is not created until the
 * {@link #install()} method is invoked.  Any attempt to access a method that requires a bundle will throw an
 * {@link org.maera.plugin.IllegalPluginStateException}.
 * <p/>
 * This class uses a {@link OsgiPluginHelper} to represent different behaviors of key methods in different states.
 * {@link OsgiPluginUninstalledHelper} implements the methods when the plugin hasn't yet been installed into the
 * OSGi container, while {@link OsgiPluginInstalledHelper} implements the methods when the bundle is available.  This
 * leaves this class to manage the {@link PluginState} and interactions with the event system.
 *
 * @since 0.1
 */
//@Threadsafe
public class OsgiPlugin extends AbstractPlugin implements AutowireCapablePlugin, ContainerManagedPlugin {
    
    private final Map<String, Element> moduleElements = new HashMap<String, Element>();
    private final PluginEventManager pluginEventManager;
    private final PackageAdmin packageAdmin;

    private final Set<OutstandingDependency> outstandingDependencies = new CopyOnWriteArraySet<OutstandingDependency>();
    private volatile boolean treatSpringBeanFactoryCreationAsRefresh = false;
    private volatile OsgiPluginHelper helper;
    public static final String SPRING_CONTEXT = "Spring-Context";
    public static final String MAERA_PLUGIN_KEY = "Maera-Plugin-Key";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final BundleListener bundleStopListener;

    public OsgiPlugin(final String key, final OsgiContainerManager mgr, final PluginArtifact artifact, final PluginEventManager pluginEventManager) {
        Validate.notNull(key, "The plugin key is required");
        Validate.notNull(mgr, "The osgi container is required");
        Validate.notNull(artifact, "The osgi container is required");
        Validate.notNull(pluginEventManager, "The osgi container is required");

        this.helper = new OsgiPluginUninstalledHelper(key, mgr, artifact);
        this.pluginEventManager = pluginEventManager;
        this.packageAdmin = extractPackageAdminFromOsgi(mgr);
        this.bundleStopListener = new SynchronousBundleListener() {
            public void bundleChanged(final BundleEvent bundleEvent) {
                if ((bundleEvent.getBundle() == getBundle()) && (bundleEvent.getType() == BundleEvent.STOPPING)) {
                    helper.onDisable();
                    setPluginState(PluginState.DISABLED);
                }
            }
        };
    }

    /**
     * Only used for testing
     *
     * @param helper The helper to use
     */
    OsgiPlugin(PluginEventManager pluginEventManager, OsgiPluginHelper helper) {
        this.helper = helper;
        this.pluginEventManager = pluginEventManager;
        this.packageAdmin = null;
        this.bundleStopListener = null;
    }

    /**
     * @return The active bundle
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public Bundle getBundle() throws IllegalPluginStateException {
        return helper.getBundle();
    }

    /**
     * @return true
     */
    public boolean isUninstallable() {
        return true;
    }

    /**
     * @return true
     */
    public boolean isDynamicallyLoaded() {
        return true;
    }

    /**
     * @return true
     */
    public boolean isDeleteable() {
        return true;
    }

    /**
     * @param clazz        The name of the class to be loaded
     * @param callingClass The class calling the loading (used to help find a classloader)
     * @param <T>          The class type
     * @return The class instance, loaded from the OSGi bundle
     * @throws ClassNotFoundException      If the class cannot be found
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException, IllegalPluginStateException {
        return helper.loadClass(clazz, callingClass);
    }

    /**
     * @param name The resource name
     * @return The resource URL, null if not found
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public URL getResource(final String name) throws IllegalPluginStateException {
        return helper.getResource(name);
    }

    /**
     * @param name The name of the resource to be loaded.
     * @return Null if not found
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public InputStream getResourceAsStream(final String name) throws IllegalPluginStateException {
        return helper.getResourceAsStream(name);
    }

    /**
     * @return The classloader to load classes and resources from the bundle
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public ClassLoader getClassLoader() throws IllegalPluginStateException {
        return helper.getClassLoader();
    }

    /**
     * Called when the spring context for the bundle has failed to be created.  This means the bundle is still
     * active, but the Spring context is not available, so for our purposes, the plugin shouldn't be enabled.
     *
     * @param event The plugin container failed event
     * @throws org.maera.plugin.IllegalPluginStateException
     *          If the plugin key hasn't been set yet
     */
    @PluginEventListener
    public void onSpringContextFailed(final PluginContainerFailedEvent event) throws IllegalPluginStateException {
        if (getKey() == null) {
            throw new IllegalPluginStateException("Plugin key must be set");
        }
        if (getKey().equals(event.getPluginKey())) {
            logAndClearOustandingDependencies();
            // TODO: do something with the exception more than logging
            getLog().error("Unable to start the Spring context for plugin " + getKey(), event.getCause());
            setPluginState(PluginState.DISABLED);
        }
    }

    @PluginEventListener
    public void onServiceDependencyWaitStarting(PluginServiceDependencyWaitStartingEvent event) {
        if (event.getPluginKey() != null && event.getPluginKey().equals(getKey())) {
            OutstandingDependency dep = new OutstandingDependency(event.getBeanName(), String.valueOf(event.getFilter()));
            outstandingDependencies.add(dep);
            getLog().info(generateOutstandingDependencyLogMessage(dep, "Waiting for"));
        }
    }

    @PluginEventListener
    public void onServiceDependencyWaitEnded(PluginServiceDependencyWaitEndedEvent event) {
        if (event.getPluginKey() != null && event.getPluginKey().equals(getKey())) {
            OutstandingDependency dep = new OutstandingDependency(event.getBeanName(), String.valueOf(event.getFilter()));
            outstandingDependencies.remove(dep);
            getLog().info(generateOutstandingDependencyLogMessage(dep, "Found"));
        }
    }

    @PluginEventListener
    public void onServiceDependencyWaitEnded(PluginServiceDependencyWaitTimedOutEvent event) {
        if (event.getPluginKey() != null && event.getPluginKey().equals(getKey())) {
            OutstandingDependency dep = new OutstandingDependency(event.getBeanName(), String.valueOf(event.getFilter()));
            outstandingDependencies.remove(dep);
            getLog().error(generateOutstandingDependencyLogMessage(dep, "Timeout waiting for "));
        }
    }

    private String generateOutstandingDependencyLogMessage(OutstandingDependency dep, String action) {
        StringBuilder sb = new StringBuilder();
        sb.append(action).append(" ");
        sb.append("service '").append(dep.getBeanName()).append("' for plugin '").append(getKey()).append("' with filter ").append(dep.getFilter());
        return sb.toString();
    }

    /**
     * Called when the spring context for the bundle has been created or refreshed.  If this is the first time the
     * context has been refreshed, then it is a new context.  Otherwise, this means that the bundle has been reloaded,
     * usually due to a dependency upgrade.
     *
     * @param event The event
     * @throws org.maera.plugin.IllegalPluginStateException
     *          If the plugin key hasn't been set yet
     */
    @PluginEventListener
    public void onSpringContextRefresh(final PluginContainerRefreshedEvent event) throws IllegalPluginStateException {
        if (getKey() == null) {
            throw new IllegalPluginStateException("Plugin key must be set");
        }
        if (getKey().equals(event.getPluginKey())) {
            outstandingDependencies.clear();
            helper.setPluginContainer(event.getContainer());
            if (!compareAndSetPluginState(PluginState.ENABLING, PluginState.ENABLED) &&
                    getPluginState() != PluginState.ENABLED) {
                log.warn("Ignoring the Spring context that was just created for plugin " + getKey() + ".  The plugin " +
                        "is in an invalid state, " + getPluginState() + ", that doesn't support a transition to " +
                        "enabled.  Most likely, it was disabled due to a timeout.");
                helper.setPluginContainer(null);
                return;
            }

            // Only send refresh event on second creation
            if (treatSpringBeanFactoryCreationAsRefresh) {
                pluginEventManager.broadcast(new PluginRefreshedEvent(this));
            } else {
                treatSpringBeanFactoryCreationAsRefresh = true;
            }
        }
    }

    /**
     * Creates and autowires the class, using Spring's autodetection algorithm
     *
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public <T> T autowire(final Class<T> clazz) throws IllegalPluginStateException {
        return autowire(clazz, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    /**
     * Creates and autowires the class
     *
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public <T> T autowire(final Class<T> clazz, final AutowireStrategy autowireStrategy) throws IllegalPluginStateException {
        return helper.autowire(clazz, autowireStrategy);
    }

    /**
     * Autowires the instance using Spring's autodetection algorithm
     *
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public void autowire(final Object instance) throws IllegalStateException {
        autowire(instance, AutowireStrategy.AUTOWIRE_AUTODETECT);
    }

    /**
     * Autowires the instance
     *
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    public void autowire(final Object instance, final AutowireStrategy autowireStrategy) throws IllegalPluginStateException {
        helper.autowire(instance, autowireStrategy);
    }

    /**
     * Determines which plugins are required for this one to operate based on tracing the "wires" or packages that
     * are imported by this plugin.  Bundles that provide those packages are determined to be required plugins.
     *
     * @return A set of bundle symbolic names, or plugin keys.  Empty set if none.
     * @since 2.2.0
     */
    @Override
    public Set<String> getRequiredPlugins() throws IllegalPluginStateException {
        return helper.getRequiredPlugins();
    }

    @Override
    public String toString() {
        return getKey();
    }

    /**
     * Installs the plugin artifact into OSGi
     *
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    @Override
    protected void installInternal() throws IllegalPluginStateException {
        Bundle bundle = helper.install();
        helper = new OsgiPluginInstalledHelper(bundle, packageAdmin);
    }

    /**
     * Enables the plugin by setting the OSGi bundle state to enabled.
     *
     * @return {@link PluginState#ENABLED}if spring isn't necessory or {@link PluginState#ENABLING} if we are waiting
     *         on a spring context
     * @throws OsgiContainerException      If the underlying OSGi system threw an exception or we tried to enable the bundle
     *                                     when it was in an invalid state
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    @Override
    protected synchronized PluginState enableInternal() throws OsgiContainerException, IllegalPluginStateException {
        PluginState stateResult;
        try {
            if (getBundle().getState() == Bundle.ACTIVE) {
                stateResult = PluginState.ENABLED;
            } else if ((getBundle().getState() == Bundle.RESOLVED) || (getBundle().getState() == Bundle.INSTALLED)) {
                pluginEventManager.register(this);
                if (!treatSpringBeanFactoryCreationAsRefresh) {
                    stateResult = PluginState.ENABLING;
                    // Set it immediately, since the Spring context refresh event could happen at any time
                    setPluginState(stateResult);
                } else {
                    stateResult = PluginState.ENABLED;
                }
                getBundle().start();
                final BundleContext ctx = getBundle().getBundleContext();
                helper.onEnable(
                        new ServiceTracker(ctx, ModuleDescriptor.class.getName(),
                                new ModuleDescriptorServiceTrackerCustomizer(this, pluginEventManager)),
                        new ServiceTracker(ctx, ListableModuleDescriptorFactory.class.getName(),
                                new UnrecognizedModuleDescriptorServiceTrackerCustomizer(this, pluginEventManager)));

                // ensure the bean factory is removed when the bundle is stopped
                ctx.addBundleListener(bundleStopListener);
            } else {
                throw new OsgiContainerException("Cannot enable the plugin '" + getKey() + "' when the bundle is not in the resolved or installed state: "
                        + getBundle().getState() + "(" + getBundle().getBundleId() + ")");
            }

            // Only set state to enabling if it hasn't already been enabled via another thread notifying of a spring
            // application context creation during the execution of this method
            return (getPluginState() != PluginState.ENABLED ? stateResult : PluginState.ENABLED);
        }
        catch (final BundleException e) {
            log.error("Detected an error (BundleException) enabling the plugin '" + getKey() + "' : " + e.getMessage() + ". " +
                    " This error usually occurs when your plugin imports a package from another bundle with a specific version constraint " +
                    "and either the bundle providing that package doesn't meet those version constraints, or there is no bundle " +
                    "available that provides the specified package. For more details on how to fix this, see " +
                    "http://confluence.atlassian.com/x/1xy6D");
            throw new OsgiContainerException("Cannot start plugin: " + getKey(), e);
        }
    }

    /**
     * Disables the plugin by changing the bundle state back to resolved
     *
     * @throws OsgiContainerException      If the OSGi system threw an exception
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    @Override
    protected synchronized void disableInternal() throws OsgiContainerException, IllegalPluginStateException {
        try {
            // Only disable underlying bundle if this is a truly dynamic plugin
            if (!PluginUtils.doesPluginRequireRestart(this)) {
                if (getPluginState() == PluginState.ENABLING) {
                    logAndClearOustandingDependencies();
                }
                helper.onDisable();
                pluginEventManager.unregister(this);
                getBundle().stop();
                treatSpringBeanFactoryCreationAsRefresh = false;
            }
        }
        catch (final BundleException e) {
            throw new OsgiContainerException("Cannot stop plugin: " + getKey(), e);
        }
    }

    private void logAndClearOustandingDependencies() {
        for (OutstandingDependency dep : outstandingDependencies) {
            getLog().error(generateOutstandingDependencyLogMessage(dep, "Never resolved"));
        }
        outstandingDependencies.clear();
    }

    /**
     * Uninstalls the bundle from the OSGi container
     *
     * @throws OsgiContainerException      If the underlying OSGi system threw an exception
     * @throws IllegalPluginStateException if the bundle hasn't been created yet
     */
    @Override
    protected void uninstallInternal() throws OsgiContainerException, IllegalPluginStateException {
        try {
            if (getBundle().getState() != Bundle.UNINSTALLED) {
                pluginEventManager.unregister(this);
                getBundle().uninstall();
                helper.onUninstall();
                setPluginState(PluginState.UNINSTALLED);
            }
        }
        catch (final BundleException e) {
            throw new OsgiContainerException("Cannot uninstall bundle " + getBundle().getSymbolicName());
        }
    }

    /**
     * Adds a module descriptor XML element for later processing, needed for dynamic module support
     *
     * @param key     The module key
     * @param element The module element
     */
    void addModuleDescriptorElement(final String key, final Element element) {
        moduleElements.put(key, element);
    }

    /**
     * Exposes {@link #removeModuleDescriptor(String)} for package-protected classes
     *
     * @param key The module descriptor key
     */
    void clearModuleDescriptor(String key) {
        removeModuleDescriptor(key);
    }

    /**
     * Gets the module elements for dynamic module descriptor handling.  Doesn't need to return a copy or anything
     * immutable because it is only accessed by package-private helper classes
     *
     * @return The map of module keys to module XML elements
     */
    Map<String, Element> getModuleElements() {
        return moduleElements;
    }

    /**
     * Extracts the {@link PackageAdmin} instance from the OSGi container
     *
     * @param mgr The OSGi container manager
     * @return The package admin instance, should never be null
     */
    private PackageAdmin extractPackageAdminFromOsgi(OsgiContainerManager mgr) {
        // Get the system bundle (always bundle 0)
        Bundle bundle = mgr.getBundles()[0];

        // We assume the package admin will always be available
        final ServiceReference ref = bundle.getBundleContext()
                .getServiceReference(PackageAdmin.class.getName());
        return (PackageAdmin) bundle.getBundleContext()
                .getService(ref);
    }

    public ContainerAccessor getContainerAccessor() {
        return helper.getContainerAccessor();
    }

    private static class OutstandingDependency {
        private final String beanName;
        private final String filter;

        public OutstandingDependency(String beanName, String filter) {
            this.beanName = beanName;
            this.filter = filter;
        }

        public String getBeanName() {
            return beanName;
        }

        public String getFilter() {
            return filter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OutstandingDependency that = (OutstandingDependency) o;

            if (beanName != null ? !beanName.equals(that.beanName) : that.beanName != null) {
                return false;
            }
            if (!filter.equals(that.filter)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = beanName != null ? beanName.hashCode() : 0;
            result = 31 * result + filter.hashCode();
            return result;
        }
    }
}
