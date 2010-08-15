package org.maera.plugin.osgi.bridge;

import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginContainerFailedEvent;
import org.maera.plugin.event.events.PluginContainerRefreshedEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextEvent;
import org.springframework.osgi.context.event.OsgiBundleApplicationContextListener;
import org.springframework.osgi.context.event.OsgiBundleContextFailedEvent;
import org.springframework.osgi.context.event.OsgiBundleContextRefreshedEvent;

/**
 * Bridges key Spring DM extender events with the plugin system
 *
 * @since 2.2.0
 */
public class SpringOsgiEventBridge implements OsgiBundleApplicationContextListener {
    private final PluginEventManager pluginEventManager;

    public SpringOsgiEventBridge(PluginEventManager pluginEventManager) {
        this.pluginEventManager = pluginEventManager;
    }

    public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent evt) {
        if (evt instanceof OsgiBundleContextFailedEvent) {
            OsgiBundleContextFailedEvent e = (OsgiBundleContextFailedEvent) evt;
            pluginEventManager.broadcast(new PluginContainerFailedEvent(
                    e.getApplicationContext(),
                    PluginBundleUtils.getPluginKey(e.getBundle()),
                    e.getFailureCause()));
        } else if (evt instanceof OsgiBundleContextRefreshedEvent) {
            OsgiBundleContextRefreshedEvent e = (OsgiBundleContextRefreshedEvent) evt;
            pluginEventManager.broadcast(new PluginContainerRefreshedEvent(
                    e.getApplicationContext(),
                    PluginBundleUtils.getPluginKey(e.getBundle())));
        }
    }
}
