package org.maera.plugin.osgi.factory;

import org.apache.commons.lang.Validate;
import org.maera.plugin.AutowireCapablePlugin;
import org.maera.plugin.IllegalPluginStateException;
import org.maera.plugin.module.ContainerAccessor;
import org.maera.plugin.osgi.container.OsgiContainerException;
import org.maera.plugin.osgi.spring.DefaultSpringContainerAccessor;
import org.maera.plugin.osgi.spring.SpringContainerAccessor;
import org.maera.plugin.osgi.util.BundleClassLoaderAccessor;
import org.maera.plugin.osgi.util.OsgiHeaderUtil;
import org.maera.plugin.util.resource.AlternativeDirectoryResourceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class that implements the methods assuming the OSGi plugin has been installed
 *
 * @since 2.2.0
 */
class OsgiPluginInstalledHelper implements OsgiPluginHelper {
    private final ClassLoader bundleClassLoader;
    private final Bundle bundle;
    private final PackageAdmin packageAdmin;

    private volatile SpringContainerAccessor containerAccessor;
    private volatile ServiceTracker[] serviceTrackers;

    /**
     * @param bundle       The bundle
     * @param packageAdmin The package admin
     */
    public OsgiPluginInstalledHelper(final Bundle bundle, final PackageAdmin packageAdmin) {
        Validate.notNull(bundle);
        Validate.notNull(packageAdmin);
        this.bundle = bundle;
        bundleClassLoader = BundleClassLoaderAccessor.getClassLoader(bundle, new AlternativeDirectoryResourceLoader());
        this.packageAdmin = packageAdmin;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException {
        return BundleClassLoaderAccessor.loadClass(getBundle(), clazz);
    }

    public URL getResource(final String name) {
        return bundleClassLoader.getResource(name);
    }

    public InputStream getResourceAsStream(final String name) {
        return bundleClassLoader.getResourceAsStream(name);
    }

    public ClassLoader getClassLoader() {
        return bundleClassLoader;
    }

    public Bundle install() {
        throw new IllegalPluginStateException("Plugin '" + bundle.getSymbolicName() + "' has already been installed");
    }

    public void onEnable(final ServiceTracker... serviceTrackers) throws OsgiContainerException {
        Validate.notNull(serviceTrackers);

        for (final ServiceTracker svc : serviceTrackers) {
            svc.open();
        }

        this.serviceTrackers = serviceTrackers;
    }

    public void onDisable() throws OsgiContainerException {
        final ServiceTracker[] serviceTrackers = this.serviceTrackers; // cache a copy locally for multi-threaded goodness
        if (serviceTrackers != null) {
            for (final ServiceTracker svc : serviceTrackers) {
                svc.close();
            }
            this.serviceTrackers = null;
        }
        setPluginContainer(null);
    }

    public void onUninstall() throws OsgiContainerException {
    }

    public <T> T autowire(final Class<T> clazz, final AutowireCapablePlugin.AutowireStrategy autowireStrategy) throws IllegalPluginStateException {
        assertSpringContextAvailable();
        return containerAccessor.createBean(clazz);
    }

    /**
     * If spring is required, it looks for the spring application context and calls autowire().  If not, the object
     * is untouched.
     *
     * @param instance         The instance to autowire
     * @param autowireStrategy The autowire strategy to use The strategy to use, only respected if spring is available
     * @return The autowired instance
     * @throws IllegalPluginStateException If spring is required but not available
     */
    public void autowire(final Object instance, final AutowireCapablePlugin.AutowireStrategy autowireStrategy) throws IllegalPluginStateException {
        assertSpringContextAvailable();
        containerAccessor.autowireBean(instance, autowireStrategy);
    }

    public Set<String> getRequiredPlugins() {
        final Set<String> keys = new HashSet<String>();
        getRequiredPluginsFromExports(keys);

        // we can't get required plugins from services, since services could have different cardinalities and you can't
        // detect that from looking at the service reference.
        return keys;
    }

    private void getRequiredPluginsFromExports(Set<String> keys) {
        // Get a set of all packages that this plugin imports
        final Set<String> imports = OsgiHeaderUtil.parseHeader((String) getBundle().getHeaders().get(Constants.IMPORT_PACKAGE)).keySet();

        // For each import, determine what bundle provides the package
        for (final String imp : imports) {
            // Get a list of package exports for this package
            final ExportedPackage[] exports = packageAdmin.getExportedPackages(imp);
            if (exports != null) {
                // For each exported package, determine if we are a consumer
                for (final ExportedPackage export : exports) {
                    // Get a list of bundles that consume that package
                    final Bundle[] importingBundles = export.getImportingBundles();
                    if (importingBundles != null) {
                        // For each importing bundle, determine if it is us
                        for (final Bundle importingBundle : importingBundles) {
                            // If we are the bundle consumer, or importer, then add the exporter as a required plugin
                            if (getBundle() == importingBundle) {
                                keys.add(OsgiHeaderUtil.getPluginKey(export.getExportingBundle()));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void setPluginContainer(final Object container) {
        if (container == null) {
            containerAccessor = null;
        } else {
            containerAccessor = new DefaultSpringContainerAccessor(container);
        }
    }

    public ContainerAccessor getContainerAccessor() {
        return containerAccessor;
    }

    /**
     * @throws IllegalPluginStateException if the spring context is not initialized
     */
    private void assertSpringContextAvailable() throws IllegalPluginStateException {
        if (containerAccessor == null) {
            throw new IllegalStateException("Cannot autowire object because the Spring context is unavailable.  " +
                    "Ensure your OSGi bundle contains the 'Spring-Context' header.");
        }
    }

}