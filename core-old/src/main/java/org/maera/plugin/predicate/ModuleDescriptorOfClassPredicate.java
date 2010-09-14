package org.maera.plugin.predicate;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.maera.plugin.ModuleDescriptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules for which their descriptor is an instance of one of the given {@link Class}.
 */
public class ModuleDescriptorOfClassPredicate<T> implements ModuleDescriptorPredicate<T> {
    private final Collection<Class<? extends ModuleDescriptor<? extends T>>> moduleDescriptorClasses;

    public ModuleDescriptorOfClassPredicate(final Class<? extends ModuleDescriptor<? extends T>> moduleDescriptorClass) {
        moduleDescriptorClasses = Collections.<Class<? extends ModuleDescriptor<? extends T>>>singleton(moduleDescriptorClass);
    }

    /**
     * @throws IllegalArgumentException if the moduleDescriptorClasses is <code>null</code>
     */
    public ModuleDescriptorOfClassPredicate(final Class<? extends ModuleDescriptor<? extends T>>[] moduleDescriptorClasses) {
        if (moduleDescriptorClasses == null) {
            throw new IllegalArgumentException("Module descriptor classes array should not be null when constructing ModuleOfClassPredicate!");
        }
        this.moduleDescriptorClasses = Arrays.asList(moduleDescriptorClasses);
    }

    public boolean matches(final ModuleDescriptor<? extends T> moduleDescriptor) {
        return (moduleDescriptor != null) && CollectionUtils.exists(moduleDescriptorClasses, new Predicate() {
            public boolean evaluate(final Object object) {
                return (object != null) && ((Class<?>) object).isInstance(moduleDescriptor);
            }
        });
    }
}
