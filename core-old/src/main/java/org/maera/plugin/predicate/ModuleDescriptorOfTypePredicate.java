package org.maera.plugin.predicate;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.ModuleDescriptorFactory;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules for which their descriptor is the given type.
 */
public class ModuleDescriptorOfTypePredicate<M> extends ModuleDescriptorOfClassPredicate<M> {
    @SuppressWarnings("unchecked")
    public ModuleDescriptorOfTypePredicate(final ModuleDescriptorFactory moduleDescriptorFactory, final String moduleDescriptorType) {
        super((Class<? extends ModuleDescriptor<? extends M>>) moduleDescriptorFactory.getModuleDescriptorClass(moduleDescriptorType));
    }
}
