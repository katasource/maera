package org.maera.plugin.osgi.external;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.hostcontainer.HostContainer;

import java.util.Collections;
import java.util.Set;

/**
 * A single module descriptor factory for plugins to use when they want to expose just one plugin.  Uses
 * {@link HostContainer} to optionally provide autowiring for new descriptor instances.
 *
 * @since 2.1
 */
public class SingleModuleDescriptorFactory<T extends ModuleDescriptor<?>> implements ListableModuleDescriptorFactory {
    private final String type;
    private final Class<T> moduleDescriptorClass;
    private final HostContainer hostContainer;

    /**
     * Constructs an instance using a specific host container
     *
     * @param hostContainer         The host container to use to create descriptor instances
     * @param type                  The type of module
     * @param moduleDescriptorClass The descriptor class
     * @since 2.2.0
     */
    public SingleModuleDescriptorFactory(final HostContainer hostContainer, final String type, final Class<T> moduleDescriptorClass) {
        this.moduleDescriptorClass = moduleDescriptorClass;
        this.type = type;
        this.hostContainer = hostContainer;
    }

    public ModuleDescriptor getModuleDescriptor(final String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        T result = null;
        if (this.type.equals(type)) {
            // We can't use an autowired bean factory to create the instance because it would be loaded by this class's
            // classloader, which will not have access to the spring instance in bundle space.
            result = hostContainer.create(moduleDescriptorClass);
        }
        return result;
    }

    public boolean hasModuleDescriptor(final String type) {
        return (this.type.equals(type));
    }

    @SuppressWarnings("unchecked")
    public Class<? extends ModuleDescriptor<?>> getModuleDescriptorClass(final String type) {
        return (this.type.equals(type) ? moduleDescriptorClass : null);
    }

    @SuppressWarnings("unchecked")
    public Set<Class<ModuleDescriptor<?>>> getModuleDescriptorClasses() {
        return Collections.singleton((Class<ModuleDescriptor<?>>) moduleDescriptorClass);
    }

    public HostContainer getHostContainer() {
        return hostContainer;
    }
}
