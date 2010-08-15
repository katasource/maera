package org.maera.plugin.osgi.factory.descriptor;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;

/**
 * Module descriptor for dynamic module types.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 2.2.0
 */
public class ModuleTypeModuleDescriptor extends AbstractModuleDescriptor<Void> {
    public ModuleTypeModuleDescriptor() {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    public Void getModule() {
        throw new UnsupportedOperationException();
    }

}
