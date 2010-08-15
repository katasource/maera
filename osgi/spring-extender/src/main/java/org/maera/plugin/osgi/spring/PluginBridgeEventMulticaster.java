package org.maera.plugin.osgi.spring;

import org.osgi.framework.*;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEventMulticasterAdapter;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;

/**
 * Finds ApplicationListener bridge and uses it to CC all event broadcasts
 *
 * @since 2.2.1
 */
public class PluginBridgeEventMulticaster extends OsgiBundleApplicationContextEventMulticasterAdapter implements BundleContextAware {
    private volatile OsgiBundleApplicationContextListener bridgeListener;

    public PluginBridgeEventMulticaster() {
        super(new SimpleApplicationEventMulticaster());
    }


    @Override
    public void multicastEvent(OsgiBundleApplicationContextEvent applicationEvent) {
        super.multicastEvent(applicationEvent);
        if (bridgeListener != null)
            bridgeListener.onOsgiApplicationEvent(applicationEvent);
    }

    /**
     * Look for the application listener bridge from maera-plugins-osgi-bridge.  Can't use Spring DM stuff as it
     * creates a circular dependency.
     *
     * @param bundleContext The bundle context for Spring DM extender
     */
    public void setBundleContext(final BundleContext bundleContext) {
        String filter = "(&(objectClass=" + OsgiBundleApplicationContextListener.class.getName() + ")(plugin-bridge=true))";

        ServiceReference[] refs;
        try {
            refs = bundleContext.getAllServiceReferences(ApplicationListener.class.getName(), filter);
            if (refs != null && refs.length == 1) {
                bridgeListener = (OsgiBundleApplicationContextListener) bundleContext.getService(refs[0]);
            }

            // Add listener to catch the extremely rare case of a late deployment or upgrade
            bundleContext.addServiceListener(new ServiceListener() {

                public void serviceChanged(ServiceEvent serviceEvent) {
                    switch (serviceEvent.getType()) {
                        case ServiceEvent.REGISTERED:
                            bridgeListener = (OsgiBundleApplicationContextListener) bundleContext.getService(serviceEvent.getServiceReference());
                            break;
                        case ServiceEvent.UNREGISTERING:
                            bridgeListener = null;
                            break;
                        case ServiceEvent.MODIFIED:
                            bridgeListener = (OsgiBundleApplicationContextListener) bundleContext.getService(serviceEvent.getServiceReference());
                            break;
                    }
                }
            }, filter);
        }
        catch (InvalidSyntaxException e) {
            // Should never happen
            throw new RuntimeException("Invalid LDAP filter", e);
        }
    }
}
