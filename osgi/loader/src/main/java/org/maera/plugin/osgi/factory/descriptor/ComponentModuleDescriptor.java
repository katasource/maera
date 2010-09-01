package org.maera.plugin.osgi.factory.descriptor;

import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.descriptors.CannotDisable;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.osgi.module.BeanPrefixModuleFactory;

/**
 * Module descriptor for Spring components.  Shouldn't be directly used outside providing read-only information.
 *
 * @since 0.1
 */
@CannotDisable
public class ComponentModuleDescriptor<Object> extends AbstractModuleDescriptor {
    
    public ComponentModuleDescriptor() {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    @Override
    protected void loadClass(Plugin plugin, String clazz) throws PluginParseException {
        // do nothing
    }

    @Override
    public Object getModule() {
        return (Object) new BeanPrefixModuleFactory().createModule(getKey(), this);
    }

    /**
     * @return Module Class Name
     * @since 2.3.0
     * @deprecated - BEWARE that this is a temporary method that will not exist for long. Deprecated since 2.3.0
     */
    public String getModuleClassName() {
        return moduleClassName;
    }
}
