package org.maera.plugin.descriptors;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.PluginParseException;

/**
 * Module descriptor factory that checks multiple factories in sequence.  There is no attempt at caching the results.
 *
 * @since 2.1
 */
public class ChainModuleDescriptorFactory implements ModuleDescriptorFactory {
    private final ModuleDescriptorFactory[] factories;

    public ChainModuleDescriptorFactory(final ModuleDescriptorFactory... factories) {
        this.factories = factories;
    }

    public ModuleDescriptor<?> getModuleDescriptor(final String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        for (final ModuleDescriptorFactory factory : factories) {
            if (factory.hasModuleDescriptor(type)) {
                return factory.getModuleDescriptor(type);
            }
        }
        return null;
    }

    public boolean hasModuleDescriptor(final String type) {
        for (final ModuleDescriptorFactory factory : factories) {
            if (factory.hasModuleDescriptor(type)) {
                return true;
            }
        }
        return false;
    }

    public Class<? extends ModuleDescriptor> getModuleDescriptorClass(final String type) {
        for (final ModuleDescriptorFactory factory : factories) {
            final Class<? extends ModuleDescriptor> descriptorClass = factory.getModuleDescriptorClass(type);
            if (descriptorClass != null) {
                return descriptorClass;
            }
        }
        return null;
    }
}
