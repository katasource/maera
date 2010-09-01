package org.maera.plugin.osgi.bridge;

import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextEvent;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextListener;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleContextFailedEvent;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleContextRefreshedEvent;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginContainerFailedEvent;
import org.maera.plugin.event.events.PluginContainerRefreshedEvent;

/**
 * Bridges key Spring DM extender events with the plugin system
 *
 * @since 0.1
 */
public class SpringOsgiEventBridge implements OsgiBundleApplicationContextListener {

    private final PluginEventManager pluginEventManager;

    public SpringOsgiEventBridge(PluginEventManager pluginEventManager) {
        this.pluginEventManager = pluginEventManager;
    }

    public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent evt) {

        if (evt instanceof OsgiBundleContextFailedEvent) {
            OsgiBundleContextFailedEvent e = (OsgiBundleContextFailedEvent) evt;
            //noinspection ThrowableResultOfMethodCallIgnored
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
