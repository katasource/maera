package net.maera.osgi.container.impl;

import net.maera.io.Resource;
import net.maera.osgi.util.OsgiHeaderUtils;
import net.maera.util.FileUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/**
 * @since 0.1
 */
public class DefaultHostActivator implements HostActivator, BundleListener, FrameworkListener {

    private static final long PACKAGE_REFRESH_TIMEOUT_MILLIS = 10 * 1000; //10 seconds

    private static final transient Logger log = LoggerFactory.getLogger(DefaultHostActivator.class);
    //private DefaultComponentRegistrar registrar;
    //private List<HostComponentRegistration> hostComponentRegistrations;

    private BundleContext bundleContext;
    private List<ServiceRegistration> hostServiceReferences;
    private Resource initialBundlesExtractionDirectory;
    
    private Resource initialBundlesLocation;
    private ClassLoader initializedClassLoader;
    private PackageAdmin packageAdmin;

    private static final FilenameFilter JAR_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    public DefaultHostActivator() {
        this.initializedClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void bundleChanged(BundleEvent evt) {
        //no need to process if we're not logging at the expected level:
        if (!log.isInfoEnabled()) {
            return;
        }

        //expected level is enabled, so construct the log message:
        String action = null;
        switch (evt.getType()) {
            case BundleEvent.INSTALLED:
                action = "Installed";
                break;
            case BundleEvent.STARTED:
                action = "Started";
                break;
            case BundleEvent.STOPPED:
                action = "Stopped";
                break;
            case BundleEvent.UPDATED:
                action = "Updated";
                break;
            case BundleEvent.UNINSTALLED:
                action = "Uninstalled";
                break;
            case BundleEvent.RESOLVED:
                action = "Resolved";
                break;
            case BundleEvent.UNRESOLVED:
                action = "Unresolved";
                break;
        }
        if (action != null) {
            Bundle bundle = evt.getBundle();
            Object[] args = {action, bundle.getSymbolicName(), bundle.getBundleId()};
            log.debug("{} bundle {} ({})", args);
        }
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    @Override
    public void frameworkEvent(FrameworkEvent event) {
        String bundleBits = "";
        if (event.getBundle() != null) {
            bundleBits = " in bundle " + event.getBundle().getSymbolicName();
        }
        switch (event.getType()) {
            case FrameworkEvent.ERROR:
                log.error("Framework error" + bundleBits, event.getThrowable());
                break;
            case FrameworkEvent.WARNING:
                log.warn("Framework warning" + bundleBits, event.getThrowable());
                break;
            case FrameworkEvent.INFO:
                log.info("Framework info" + bundleBits, event.getThrowable());
                break;
        }
    }

    public Bundle[] getBundles() {
        return bundleContext.getBundles();
    }

    @Override
    public ServiceTracker getServiceTracker(String serviceClassName, final CloseCallback callback) {
        ServiceTracker tracker = new ServiceTracker(bundleContext, serviceClassName, null) {

            @Override
            public void close() {
                if (callback != null) {
                    callback.onClose(this);
                }
            }
        };
        tracker.open();
        return tracker;
    }

    public Bundle install(final File path, final boolean uninstallOtherVersions) throws BundleException {
        boolean bundleUninstalled = false;
        if (uninstallOtherVersions) {
            try {
                JarFile jar = new JarFile(path);
                String pluginKey = null;
                try {
                    pluginKey = OsgiHeaderUtils.getPluginKey(jar.getManifest());
                } finally {
                    jar.close();
                }
                for (final Bundle oldBundle : bundleContext.getBundles()) {
                    if (pluginKey.equals(OsgiHeaderUtils.getPluginKey(oldBundle))) {
                        log.info("Uninstalling existing version " + oldBundle.getHeaders().get(Constants.BUNDLE_VERSION));
                        oldBundle.uninstall();
                        bundleUninstalled = true;
                    }
                }
            } catch (final IOException e) {
                throw new BundleException("Invalid bundle format", e);
            }
        }
        final Bundle bundle = bundleContext.installBundle(path.toURI().toString());
        if (bundleUninstalled) {
            refreshPackages();
        }
        return bundle;
    }

    @Override
    public Bundle installBundle(Resource bundleResource, boolean uninstallPrevious) throws BundleException {
        try {
            return install(bundleResource.getFile(), true);
        } catch (IOException e) {
            throw new BundleException("Unable to obtain file from bundle Resource " + bundleResource, e);
        }
    }

    public void refreshPackages() {
        final CountDownLatch latch = new CountDownLatch(1);


        FrameworkListener refreshListener = new FrameworkListener() {

            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
                    log.info("Packages refreshed");
                    latch.countDown();
                }
            }
        };

        bundleContext.addFrameworkListener(refreshListener);
        try {
            packageAdmin.refreshPackages(null);
            boolean refreshed = false;
            try {
                refreshed = latch.await(PACKAGE_REFRESH_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                //_always_ preserve the interrupt status if you don't re-throw the InterruptedException
                //(Java Concurrency in Practice, Section 7.1.2):
                Thread.currentThread().interrupt();
            }
            if (!refreshed) {
                log.warn("Timeout exceeded waiting for package refresh");
            }
        }
        finally {
            bundleContext.removeFrameworkListener(refreshListener);
        }
    }

    public void start(final BundleContext context) throws Exception {
        if (this.initialBundlesLocation == null) {
            throw new BundleException("initialBundlesLocation property cannot be null.");
        }
        if ( this.initialBundlesExtractionDirectory == null) {
            throw new BundleException("initialBundlesExtractionDirectory cannot be null.");
        }

        bundleContext = context;
        final ServiceReference ref = context.getServiceReference(PackageAdmin.class.getName());
        packageAdmin = (PackageAdmin) context.getService(ref);
        context.addBundleListener(this);
        context.addFrameworkListener(this);

        //loadHostComponents(registrar);
        //TODO: re-enable:
        extractAndInstallFrameworkBundles();

        log.debug("Started activator {}", this);
    }

    public void stop(final BundleContext ctx) throws Exception {
        ctx.removeBundleListener(this);
        ctx.removeFrameworkListener(this);
        if (hostServiceReferences != null) {
            for (ServiceRegistration ref : hostServiceReferences) {
                ref.unregister();
            }
        }
        bundleContext = null;
        packageAdmin = null;
        hostServiceReferences = null;
        //hostComponentRegistrations = null;
        //registrar = null;
        initializedClassLoader = null;
        log.debug("Stopped activator {}", this);
    }

    /*public List<HostComponentRegistration> getHostComponentRegistrations() {
        return hostComponentRegistrations;
    }

    void loadHostComponents(final DefaultComponentRegistrar registrar) {
        // Unregister any existing host components
        if (hostServiceReferences != null) {
            for (final ServiceRegistration reg : hostServiceReferences) {
                reg.unregister();
            }
        }

        ClassLoaderStack.runInContext(initializedClassLoader, new Runnable() {
            public void run() {
                hostServiceReferences = registrar.writeRegistry(bundleContext);
                hostComponentRegistrations = registrar.getRegistry();
            }
        });
    }*/

    private void extractAndInstallFrameworkBundles() throws BundleException {
        final List<Bundle> bundles = new ArrayList<Bundle>();
        File extractDir = null;
        try {
            extractDir = initialBundlesExtractionDirectory.getFile();
            FileUtils.conditionallyExtractZipFile(initialBundlesLocation.getURL(), extractDir);
        } catch (IOException e) {
            throw new BundleException("Unable to extract .zip [" + initialBundlesLocation + "] to directory " +
                    "[" + initialBundlesExtractionDirectory + "]", e);
        }

        for( final File bundleFile : extractDir.listFiles(JAR_FILTER) ) {
            bundles.add(install(bundleFile, false));
        }

        packageAdmin.resolveBundles(null);

        for (final Bundle bundle : bundles) {
            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
                bundle.start();
            }
        }
    }

    public Resource getInitialBundlesExtractionDirectory() {
        return initialBundlesExtractionDirectory;
    }

    public void setInitialBundlesExtractionDirectory(Resource initialBundlesExtractionDirectory) {
        this.initialBundlesExtractionDirectory = initialBundlesExtractionDirectory;
    }

    public Resource getInitialBundlesLocation() {
        return initialBundlesLocation;
    }

    public void setInitialBundlesLocation(Resource initialBundlesLocation) {
        this.initialBundlesLocation = initialBundlesLocation;
    }
}
