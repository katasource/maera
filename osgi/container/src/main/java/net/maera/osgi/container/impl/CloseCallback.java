package net.maera.osgi.container.impl;

import org.osgi.util.tracker.ServiceTracker;

/**
 * @since 0.1
 */
public interface CloseCallback {

    /**
     * The tracker's {@link ServiceTracker#close} method will be called before this method is invoked.
     *
     * @param tracker the closed tracker.
     */
    void onClose(ServiceTracker tracker);
}
