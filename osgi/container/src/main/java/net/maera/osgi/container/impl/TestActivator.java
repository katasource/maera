package net.maera.osgi.container.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 0.1
 */
public class TestActivator implements BundleActivator, BundleListener, FrameworkListener {

    private static final transient Logger log = LoggerFactory.getLogger(TestActivator.class);

    private BundleContext bundleContext;
    private PackageAdmin packageAdmin;
    private ClassLoader initializationClassLoader;

    public TestActivator() {
        this.initializationClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        final ServiceReference ref = context.getServiceReference(PackageAdmin.class.getName());
        packageAdmin = (PackageAdmin) context.getService(ref);
        context.addBundleListener(this);
        context.addFrameworkListener(this);
        log.info("Started activator {}", this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);
        context.removeFrameworkListener(this);
        bundleContext = null;
        packageAdmin = null;
        initializationClassLoader = null;
        log.info("Stopped activator {}", this);
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
            log.info("{} bundle {} ({})", args);
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
}
