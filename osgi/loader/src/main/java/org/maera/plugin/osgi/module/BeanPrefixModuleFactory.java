package org.maera.plugin.osgi.module;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.module.ContainerManagedPlugin;
import org.maera.plugin.module.PrefixModuleFactory;
import org.maera.plugin.osgi.spring.SpringContainerAccessor;

/**
 * The SpringBeanModuleFactory creates a java bean for the given module class by resolving the name to spring bean reference.
 * It returns a reference to this bean.
 *
 * @since 0.1
 */
public class BeanPrefixModuleFactory implements PrefixModuleFactory {
    
    public <T> T createModule(String name, ModuleDescriptor<T> moduleDescriptor) {
        if (moduleDescriptor.getPlugin() instanceof ContainerManagedPlugin) {
            ContainerManagedPlugin containerManagedPlugin = (ContainerManagedPlugin) moduleDescriptor.getPlugin();
            return (T) ((SpringContainerAccessor) containerManagedPlugin.getContainerAccessor()).getBean(name);
        } else {
            throw new IllegalArgumentException("Failed to resolve '" + name + "'. You cannot use 'bean' prefix with non-OSGi plugins");
        }
    }

    public String getPrefix() {
        return "bean";
    }
}
