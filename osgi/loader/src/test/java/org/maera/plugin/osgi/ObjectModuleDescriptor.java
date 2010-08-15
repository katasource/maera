package org.maera.plugin.osgi;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;

/**
 * Module type for an object
 */
public class ObjectModuleDescriptor extends AbstractModuleDescriptor<Object> {
    public ObjectModuleDescriptor() {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    public ObjectModuleDescriptor(ModuleFactory moduleCreator) {
        super(moduleCreator);
    }

    public Object getModule() {
        return moduleFactory.createModule(moduleClassName, this);
    }
}
