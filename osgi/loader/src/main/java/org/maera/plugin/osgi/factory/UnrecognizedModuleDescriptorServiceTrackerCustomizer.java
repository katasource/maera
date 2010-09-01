package org.maera.plugin.osgi.factory;

import org.apache.commons.lang.Validate;
import org.dom4j.Element;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.descriptors.UnrecognisedModuleDescriptor;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginModuleAvailableEvent;
import org.maera.plugin.event.events.PluginModuleUnavailableEvent;
import org.maera.plugin.osgi.external.ListableModuleDescriptorFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Service tracker that tracks {@link org.maera.plugin.osgi.external.ListableModuleDescriptorFactory} instances and handles transforming
 * {@link org.maera.plugin.descriptors.UnrecognisedModuleDescriptor}} instances into modules if the new factory supports them.  Updates to factories
 * and removal are also handled.
 *
 * @since 0.1
 */
class UnrecognizedModuleDescriptorServiceTrackerCustomizer implements ServiceTrackerCustomizer {
    
    private static final Logger log = LoggerFactory.getLogger(UnrecognizedModuleDescriptorServiceTrackerCustomizer.class);

    private final Bundle bundle;
    private final OsgiPlugin plugin;
    private final PluginEventManager pluginEventManager;

    public UnrecognizedModuleDescriptorServiceTrackerCustomizer(OsgiPlugin plugin, PluginEventManager pluginEventManager) {
        this.pluginEventManager = pluginEventManager;
        Validate.notNull(plugin);
        this.bundle = plugin.getBundle();
        Validate.notNull(bundle);
        this.plugin = plugin;
    }

    /**
     * Turns any {@link org.maera.plugin.descriptors.UnrecognisedModuleDescriptor} modules that can be handled by the new factory into real
     * modules
     */
    public Object addingService(final ServiceReference serviceReference) {
        final ListableModuleDescriptorFactory factory = (ListableModuleDescriptorFactory) bundle.getBundleContext().getService(serviceReference);

        // Only register the factory if it is or should be being used by this plugin.  We still care if they are currently
        // in use because we need to change them to unrecognized descriptors if the factory goes away.
        if (canFactoryResolveUnrecognizedDescriptor(factory) || isFactoryInUse(factory)) {
            return factory;
        } else {
            // The docs seem to indicate returning null is enough to untrack a service, but the source code and tests
            // show otherwise.
            bundle.getBundleContext().ungetService(serviceReference);
            return null;
        }
    }

    /**
     * See if the descriptor factory can resolve any unrecognized descriptors for this plugin, and if so, resolve them
     *
     * @param factory The new module descriptor factory
     * @return True if any were resolved, false otherwise
     */
    private boolean canFactoryResolveUnrecognizedDescriptor(ListableModuleDescriptorFactory factory) {
        boolean usedFactory = false;
        for (final UnrecognisedModuleDescriptor unrecognised : getModuleDescriptorsByDescriptorClass(UnrecognisedModuleDescriptor.class)) {
            final Element source = plugin.getModuleElements().get(unrecognised.getKey());
            if ((source != null) && factory.hasModuleDescriptor(source.getName())) {
                usedFactory = true;
                try {
                    final ModuleDescriptor<?> descriptor = factory.getModuleDescriptor(source.getName());
                    descriptor.init(unrecognised.getPlugin(), source);
                    plugin.addModuleDescriptor(descriptor);
                    if (log.isInfoEnabled()) {
                        log.info("Turned plugin module " + descriptor.getCompleteKey() + " into module " + descriptor);
                    }
                    pluginEventManager.broadcast(new PluginModuleAvailableEvent(descriptor));
                }
                catch (final Exception e) {
                    log.error("Unable to transform " + unrecognised.getCompleteKey() + " into actual plugin module using factory " + factory, e);
                    unrecognised.setErrorText(e.getMessage());
                }
            }
        }
        return usedFactory;
    }

    /**
     * Determine if the module descriptor factory is being used by any of the recognized descriptors.
     *
     * @param factory The new descriptor factory
     * @return True if in use, false otherwise
     */
    private boolean isFactoryInUse(ListableModuleDescriptorFactory factory) {
        for (ModuleDescriptor descriptor : plugin.getModuleDescriptors()) {
            for (Class<ModuleDescriptor<?>> descriptorClass : factory.getModuleDescriptorClasses()) {
                if (descriptorClass == descriptor.getClass()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void modifiedService(final ServiceReference serviceReference, final Object o) {
        // do nothing as it is only modifying the properties, which we largely ignore
    }

    /**
     * Reverts any current module descriptors that were provided from the factory being removed into {@link
     * UnrecognisedModuleDescriptor} instances.
     */
    public void removedService(final ServiceReference serviceReference, final Object o) {
        final ListableModuleDescriptorFactory factory = (ListableModuleDescriptorFactory) o;
        for (final Class<ModuleDescriptor<?>> moduleDescriptorClass : factory.getModuleDescriptorClasses()) {
            for (final ModuleDescriptor<?> descriptor : getModuleDescriptorsByDescriptorClass(moduleDescriptorClass)) {
                pluginEventManager.broadcast(new PluginModuleUnavailableEvent(descriptor));
                final UnrecognisedModuleDescriptor unrecognisedModuleDescriptor = new UnrecognisedModuleDescriptor();
                final Element source = plugin.getModuleElements().get(descriptor.getKey());
                if (source != null) {
                    unrecognisedModuleDescriptor.init(plugin, source);
                    unrecognisedModuleDescriptor.setErrorText(UnrecognisedModuleDescriptorFallbackFactory.DESCRIPTOR_TEXT);
                    plugin.addModuleDescriptor(unrecognisedModuleDescriptor);
                    pluginEventManager.broadcast(new PluginModuleAvailableEvent(unrecognisedModuleDescriptor));
                    if (log.isInfoEnabled()) {
                        log.info("Removed plugin module " + unrecognisedModuleDescriptor.getCompleteKey() + " as its factory was uninstalled");
                    }

                }
            }
        }
    }

    /**
     * @param descriptor
     * @param <T>
     * @return
     */
    <T extends ModuleDescriptor<?>> List<T> getModuleDescriptorsByDescriptorClass(final Class<T> descriptor) {
        final List<T> result = new ArrayList<T>();

        for (final ModuleDescriptor<?> moduleDescriptor : plugin.getModuleDescriptors()) {
            if (moduleDescriptor.getClass()
                    .isAssignableFrom(descriptor)) {
                result.add(descriptor.cast(moduleDescriptor));
            }
        }
        return result;
    }
}