package org.maera.plugin.osgi.factory;

import org.apache.commons.lang.Validate;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginModuleAvailableEvent;
import org.maera.plugin.event.events.PluginModuleUnavailableEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks module descriptors registered as services, then updates the descriptors map accordingly
 *
 * @since 0.1
 */
class ModuleDescriptorServiceTrackerCustomizer implements ServiceTrackerCustomizer {
    
    private static final Logger log = LoggerFactory.getLogger(ModuleDescriptorServiceTrackerCustomizer.class);

    private final Bundle bundle;
    private final OsgiPlugin plugin;
    private final PluginEventManager pluginEventManager;

    public ModuleDescriptorServiceTrackerCustomizer(OsgiPlugin plugin, PluginEventManager pluginEventManager) {
        this.pluginEventManager = pluginEventManager;
        Validate.notNull(plugin);
        this.bundle = plugin.getBundle();
        Validate.notNull(bundle);
        this.plugin = plugin;
    }

    public Object addingService(final ServiceReference serviceReference) {
        ModuleDescriptor<?> descriptor = null;
        if (serviceReference.getBundle() == bundle) {
            descriptor = (ModuleDescriptor<?>) bundle.getBundleContext().getService(serviceReference);
            plugin.addModuleDescriptor(descriptor);
            if (log.isInfoEnabled()) {
                log.info("Dynamically registered new module descriptor: " + descriptor.getCompleteKey());
            }
            pluginEventManager.broadcast(new PluginModuleAvailableEvent(descriptor));
        }
        return descriptor;
    }

    public void modifiedService(final ServiceReference serviceReference, final Object o) {
        // Don't bother doing anything as it only represents a change in properties
    }

    public void removedService(final ServiceReference serviceReference, final Object o) {
        if (serviceReference.getBundle() == bundle) {
            final ModuleDescriptor<?> descriptor = (ModuleDescriptor<?>) o;
            plugin.clearModuleDescriptor(descriptor.getKey());
            if (log.isInfoEnabled()) {
                log.info("Dynamically removed module descriptor: " + descriptor.getCompleteKey());
            }
            pluginEventManager.broadcast(new PluginModuleUnavailableEvent(descriptor));
        }
    }
}
