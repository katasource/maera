package org.maera.plugin.osgi.bridge;

import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.osgi.event.PluginServiceDependencyWaitEndedEvent;
import org.maera.plugin.osgi.event.PluginServiceDependencyWaitStartingEvent;
import org.maera.plugin.osgi.event.PluginServiceDependencyWaitTimedOutEvent;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitEndedEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitStartingEvent;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitTimedOutEvent;
import org.springframework.osgi.service.importer.support.AbstractOsgiServiceImportFactoryBean;

/**
 * Bridge for internal spring context events and the plugin framework event system, specifically when the internal
 * spring context is waiting for OSGi service dependencies.
 *
 * @since 2.2.1
 */
public class SpringContextEventBridge implements OsgiBundleApplicationContextListener {
    private static final Logger log = LoggerFactory.getLogger(SpringContextEventBridge.class);

    private final PluginEventManager pluginEventManager;

    public SpringContextEventBridge(PluginEventManager pluginEventManager) {
        this.pluginEventManager = pluginEventManager;
    }

    public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent osgiEvent) {
        // catch events where a manditory service waiting period is beginning
        if (osgiEvent instanceof BootstrappingDependencyEvent) {
            OsgiServiceDependencyEvent event = ((BootstrappingDependencyEvent) osgiEvent).getDependencyEvent();
            if (log.isDebugEnabled()) {
                log.debug("Handling osgi application context event: " + event);
            }

            String beanName = event.getServiceDependency()
                    .getBeanName();
            String pluginKey = null;

            // Unfortunately, the source could really be anything, so let's try the instances that we know of
            if (event.getSource() != null) {
                // maybe the source is an application context
                if (event.getSource() instanceof ConfigurableOsgiBundleApplicationContext) {
                    Bundle bundle = ((ConfigurableOsgiBundleApplicationContext) event.getSource()).getBundle();
                    pluginKey = PluginBundleUtils.getPluginKey(bundle);
                }

                // or maybe the source is a factory bean
                else {
                    if (event.getSource() instanceof AbstractOsgiServiceImportFactoryBean) {
                        AbstractOsgiServiceImportFactoryBean bean = ((AbstractOsgiServiceImportFactoryBean) event.getSource());
                        if (beanName == null) {
                            beanName = bean.getBeanName();
                        }
                        if (bean.getBundleContext() != null) {
                            pluginKey = PluginBundleUtils.getPluginKey(bean.getBundleContext().getBundle());
                        }
                    }
                }
            }

            // If the plugin key isn't found, it won't be used to provide useful messages to the plugin framework, so
            // log this so that we can fix this as we find them.
            if (pluginKey == null && log.isDebugEnabled()) {
                log.debug("Cannot determine the plugin key for event: " + event + " and source: " + event.getSource());
            }
            if (event instanceof OsgiServiceDependencyWaitStartingEvent) {
                pluginEventManager.broadcast(new PluginServiceDependencyWaitStartingEvent(
                        pluginKey,
                        beanName,
                        event.getServiceDependency().getServiceFilter(),
                        ((OsgiServiceDependencyWaitStartingEvent) event).getTimeToWait()));
            } else {
                if (event instanceof OsgiServiceDependencyWaitEndedEvent) {
                    pluginEventManager.broadcast(new PluginServiceDependencyWaitEndedEvent(
                            pluginKey,
                            beanName,
                            event.getServiceDependency().getServiceFilter(),
                            ((OsgiServiceDependencyWaitEndedEvent) event).getElapsedTime()));
                } else {
                    if (event instanceof OsgiServiceDependencyWaitTimedOutEvent) {
                        pluginEventManager.broadcast(new PluginServiceDependencyWaitTimedOutEvent(
                                pluginKey,
                                beanName,
                                event.getServiceDependency().getServiceFilter(),
                                ((OsgiServiceDependencyWaitTimedOutEvent) event).getElapsedTime()));
                    }
                }
            }
        }
    }
}
