package org.maera.plugin.descriptors;

import org.maera.plugin.module.ModuleFactory;

/**
 * @deprecated All module descriptors now have resources. Use AbstractModuleDescriptor instead.
 */
@Deprecated
public abstract class ResourcedModuleDescriptor<T> extends AbstractModuleDescriptor<T> {
    public ResourcedModuleDescriptor() {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }
}
