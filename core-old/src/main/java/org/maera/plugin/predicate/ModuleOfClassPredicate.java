package org.maera.plugin.predicate;

import org.maera.plugin.ModuleDescriptor;

/**
 * A {@link ModuleDescriptorPredicate} that matches modules that are is an instance of the given {@link Class}.
 */
public class ModuleOfClassPredicate<T> implements ModuleDescriptorPredicate<T> {
    private final Class<T> moduleClass;

    /**
     * @throws IllegalArgumentException if the moduleClass is <code>null</code>
     */
    public ModuleOfClassPredicate(final Class<T> moduleClass) {
        if (moduleClass == null) {
            throw new IllegalArgumentException("Module class should not be null when constructing ModuleOfClassPredicate!");
        }
        this.moduleClass = moduleClass;
    }

    public boolean matches(final ModuleDescriptor<? extends T> moduleDescriptor) {
        if (moduleDescriptor != null) {
            final Class<? extends T> moduleClassInDescriptor = moduleDescriptor.getModuleClass();
            return (moduleClassInDescriptor != null) && moduleClass.isAssignableFrom(moduleClassInDescriptor);
        }

        return false;
    }
}
