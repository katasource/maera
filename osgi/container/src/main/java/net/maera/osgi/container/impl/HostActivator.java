package net.maera.osgi.container.impl;

import net.maera.osgi.util.OsgiHeaderUtils;
import net.maera.util.FileUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

/**
 * @since 0.1
 */
public class HostActivator implements BundleActivator, BundleListener, FrameworkListener {

    private static final transient Logger log = LoggerFactory.getLogger(HostActivator.class);

    private static final long PACKAGE_REFRESH_TIMEOUT_MILLIS = 10 * 1000; //10 seconds

    private BundleContext bundleContext;
    private List<ServiceRegistration> hostServiceReferences;
    //private DefaultComponentRegistrar registrar;
    //private List<HostComponentRegistration> hostComponentRegistrations;
    private final URL frameworkBundlesUrl;
    private final File frameworkBundlesDir;
    private ClassLoader initializedClassLoader;
    private PackageAdmin packageAdmin;

    public HostActivator(final URL frameworkBundlesUrl, final File frameworkBundlesDir/*, final DefaultComponentRegistrar registrar*/) {
        //this.registrar = registrar;
        this.frameworkBundlesUrl = frameworkBundlesUrl;
        this.frameworkBundlesDir = frameworkBundlesDir;
        this.initializedClassLoader = Thread.currentThread().getContextClassLoader();
    }

    public void start(final BundleContext context) throws Exception {
        bundleContext = context;
        final ServiceReference ref = context.getServiceReference(org.osgi.service.packageadmin.PackageAdmin.class.getName());
        packageAdmin = (PackageAdmin) context.getService(ref);

        context.addBundleListener(this);
        context.addFrameworkListener(this);

        //loadHostComponents(registrar);
        extractAndInstallFrameworkBundles();
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
    }

    public void bundleChanged(final BundleEvent evt) {
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
            log.info("{} bundle {} ({})", args);
        }
    }

    public Bundle install(final File path, final boolean uninstallOtherVersions) throws BundleException {
        boolean bundleUninstalled = false;
        if (uninstallOtherVersions) {
            try {
                JarFile jar = new JarFile(path);
                String pluginKey = null;
                try {
                    pluginKey = OsgiHeaderUtils.getPluginKey(jar.getManifest());
                }
                finally {
                    jar.close();
                }
                for (final Bundle oldBundle : bundleContext.getBundles()) {
                    if (pluginKey.equals(OsgiHeaderUtils.getPluginKey(oldBundle))) {
                        log.info("Uninstalling existing version " + oldBundle.getHeaders().get(Constants.BUNDLE_VERSION));
                        oldBundle.uninstall();
                        bundleUninstalled = true;
                    }
                }
            }
            catch (final IOException e) {
                throw new BundleException("Invalid bundle format", e);
            }
        }
        final Bundle bundle = bundleContext.installBundle(path.toURI().toString());
        if (bundleUninstalled) {
            refreshPackages();
        }
        return bundle;
    }

    public Bundle[] getBundles() {
        return bundleContext.getBundles();
    }

    public ServiceTracker getServiceTracker(final String clazz, final Collection<ServiceTracker> trackedTrackers) {
        return new ServiceTracker(bundleContext, clazz, null) {
            @Override
            public void close() {
                trackedTrackers.remove(this);
            }
        };
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
        FileUtils.conditionallyExtractZipFile(frameworkBundlesUrl, frameworkBundlesDir);
        for (final File bundleFile : frameworkBundlesDir.listFiles(new FilenameFilter() {
            public boolean accept(final File file, final String s) {
                return s.endsWith(".jar");
            }
        })) {
            bundles.add(install(bundleFile, false));
        }

        packageAdmin.resolveBundles(null);

        for (final Bundle bundle : bundles) {
            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
                bundle.start();
            }
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

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
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
}
