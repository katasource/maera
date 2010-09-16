package net.maera.osgi.container.impl;

import net.maera.io.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @since 0.1
 */
public interface HostActivator extends BundleActivator {

    Bundle installBundle(Resource bundleResource, boolean uninstallPrevious) throws BundleException;

    void refreshPackages();

    ServiceTracker getServiceTracker(String serviceClassName, CloseCallback callback);
}
