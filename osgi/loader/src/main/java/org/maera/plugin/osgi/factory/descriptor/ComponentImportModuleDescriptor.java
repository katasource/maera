package org.maera.plugin.osgi.factory.descriptor;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.descriptors.CannotDisable;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.osgi.module.BeanPrefixModuleFactory;

/**
 * Module descriptor for OSGi service imports.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 0.1
 */
@CannotDisable
public class ComponentImportModuleDescriptor extends AbstractModuleDescriptor<Object> {

    public ComponentImportModuleDescriptor() {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    public Object getModule() {
        return new BeanPrefixModuleFactory().createModule(getKey(), this);
    }

}
