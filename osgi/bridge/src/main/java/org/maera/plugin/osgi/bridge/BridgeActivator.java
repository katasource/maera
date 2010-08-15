package org.maera.plugin.osgi.bridge;

import org.maera.plugin.event.PluginEventManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Registers services for bridging Spring events with the plugin event system
 *
 * @since 2.2.0
 */
public class BridgeActivator implements BundleActivator {
    public void start(BundleContext bundleContext) throws Exception {
        // We can do this because the plugin event manager is a host component
        ServiceReference ref = bundleContext.getServiceReference(PluginEventManager.class.getName());
        if (ref == null) {
            throw new IllegalStateException("The PluginEventManager service must be exported from the application");
        }
        PluginEventManager pluginEventManager = (PluginEventManager) bundleContext.getService(ref);

        // Register the listener for context refreshed and failed events
        bundleContext.registerService(
                OsgiBundleApplicationContextListener.class.getName(),
                new SpringOsgiEventBridge(pluginEventManager),
                null);

        // Register the listener for internal application context events like waiting for dependencies
        Dictionary<String, String> dict = new Hashtable<String, String>();
        dict.put("plugin-bridge", "true");
        bundleContext.registerService(
                OsgiBundleApplicationContextListener.class.getName(),
                new SpringContextEventBridge(pluginEventManager),
                dict);
    }

    public void stop(BundleContext bundleContext) throws Exception {
    }
}
