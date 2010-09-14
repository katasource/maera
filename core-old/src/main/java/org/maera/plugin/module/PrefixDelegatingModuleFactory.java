package org.maera.plugin.module;

import org.apache.commons.lang.Validate;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The default implementation of a {@link ModuleFactory}.
 * The module class name can contain a prefix and this prefix determines which {@link org.maera.plugin.module.ModuleFactory}
 * is used to create the java class for this module descriptor.
 * <p/>
 * If no prefix is supplied it will use {@link ClassPrefixModuleFactory} to create the module class object.
 * <p/>
 * ModuleFactories are located via the following algorithm.  First, the prefixes registered during construction are searched, then
 * any implementations in the plugin's container, if applicable.
 * <p/>
 * <i>Implementation note:</i>  The plugin's container is searched, instead of
 * a general search for all OSGi services registered against {@link PrefixModuleFactory}, because the factories
 * have to be available before any modules are created, and Spring DM, for example, ensures all required service
 * references are available before creating the context, which is then interpreted as an enabled plugin.
 *
 * @Since 2.5.0
 */
public class PrefixDelegatingModuleFactory implements ModuleFactory {
    Logger log = LoggerFactory.getLogger(PrefixDelegatingModuleFactory.class);
    private final Map<String, ModuleFactory> delegateModuleFactories;

    public PrefixDelegatingModuleFactory(Set<PrefixModuleFactory> delegates) {
        Map<String, ModuleFactory> factories = new HashMap<String, ModuleFactory>();
        for (PrefixModuleFactory factory : delegates) {
            factories.put(factory.getPrefix(), factory);
        }
        this.delegateModuleFactories = factories;
    }

    public void addPrefixModuleFactory(PrefixModuleFactory prefixModuleFactory) {
        delegateModuleFactories.put(prefixModuleFactory.getPrefix(), prefixModuleFactory);
    }

    /**
     * Returns the module factory for a prefix, first using registered prefixes, then any from the plugin's container.
     *
     * @param moduleReference  The module reference
     * @param moduleDescriptor The descriptor containing the module
     * @return The instance, can return null
     */
    protected ModuleFactory getModuleFactoryForPrefix(final ModuleReference moduleReference, ModuleDescriptor<?> moduleDescriptor) {
        ModuleFactory moduleFactory = delegateModuleFactories.get(moduleReference.prefix);
        if (moduleFactory == null) {
            Plugin plugin = moduleDescriptor.getPlugin();
            if (plugin instanceof ContainerManagedPlugin) {
                Collection<PrefixModuleFactory> containerFactories = ((ContainerManagedPlugin) plugin).getContainerAccessor().getBeansOfType(PrefixModuleFactory.class);
                for (PrefixModuleFactory prefixModuleFactory : containerFactories) {
                    if (moduleReference.prefix.equals(prefixModuleFactory.getPrefix())) {
                        moduleFactory = prefixModuleFactory;
                        break;
                    }
                }
            }
        }

        return moduleFactory;
    }


    public <T> T createModule(String className, final ModuleDescriptor<T> moduleDescriptor) throws PluginParseException {
        Validate.notNull(className, "The className cannot be null");
        Validate.notNull(moduleDescriptor, "The moduleDescriptor cannot be null");

        final ModuleReference moduleReference = getBeanReference(className);

        Object result = null;

        final ModuleFactory moduleFactory = getModuleFactoryForPrefix(moduleReference, moduleDescriptor);
        if (moduleFactory == null) {
            throw new PluginParseException("Failed to create a module. Prefix '" + moduleReference.prefix + "' not supported");
        }
        try {
            result = moduleFactory.createModule(moduleReference.beanIdentifier, moduleDescriptor);
        }
        catch (NoClassDefFoundError error) {
            log.error("Detected an error (NoClassDefFoundError) instantiating the module for plugin '" + moduleDescriptor.getPlugin().getKey() + "'" + " for module '" + moduleDescriptor.getKey() + "': " + error.getMessage() + ".  This error is usually caused by your" + " plugin using a imported component class that itself relies on other packages in the product. You can probably fix this by" + " adding the missing class's package to your <Import-Package> instructions; for more details on how to fix this, see" + " http://confluence.atlassian.com/x/QRS-Cg .");
            throw error;
        }
        catch (LinkageError error) {
            log.error("Detected an error (LinkageError) instantiating the module for plugin '" + moduleDescriptor.getPlugin().getKey() + "'" + " for module '" + moduleDescriptor.getKey() + "': " + error.getMessage() + ".  This error is usually caused by your" + " plugin including copies of libraries in META-INF/lib unnecessarily. For more details on how to fix this, see" + " http://confluence.atlassian.com/x/yQEhCw .");
            throw error;
        }
        catch (RuntimeException ex) {
            if (ex.getClass().getSimpleName().equals("UnsatisfiedDependencyException")) {
                log.error("Detected an error instantiating the module via Spring. This usually means that you haven't created a " + "<component-import> for the interface you're trying to use. See http://confluence.atlassian.com/x/kgL3CQ " + " for more details.");
            }
            throw ex;
        }

        if (result != null) {
            return (T) result;
        } else {
            throw new PluginParseException("Unable to create module instance from '" + className + "'");
        }
    }


    private ModuleReference getBeanReference(String className) {
        String prefix = "class";
        final int prefixIndex = className.indexOf(":");
        if (prefixIndex != -1) {
            prefix = className.substring(0, prefixIndex);
            className = className.substring(prefixIndex + 1);
        }
        return new ModuleReference(prefix, className);
    }

    /**
     * This is not to be used.  It is only for backwards compatibility with old code that uses
     * {@link org.maera.plugin.PluginAccessor#getEnabledModulesByClass(Class)}.  This method can and will be
     * removed without warning.
     *
     * @param name             The class name
     * @param moduleDescriptor The module descriptor
     * @param <T>              The module class type
     * @return The module class
     * @throws ModuleClassNotFoundException
     * @deprecated Since 2.5.0
     */
    @Deprecated
    public <T> Class<T> guessModuleClass(final String name, final ModuleDescriptor<T> moduleDescriptor) throws ModuleClassNotFoundException {
        Validate.notNull(name, "The class name cannot be null");
        Validate.notNull(moduleDescriptor, "The module descriptor cannot be null");

        final ModuleReference moduleReference = getBeanReference(name);

        final ModuleFactory moduleFactory = getModuleFactoryForPrefix(moduleReference, moduleDescriptor);
        Class<T> result = null;
        if (moduleFactory instanceof ClassPrefixModuleFactory) {
            result = ((ClassPrefixModuleFactory) moduleFactory).getModuleClass(moduleReference.beanIdentifier, moduleDescriptor);
        }

        return result;

    }

    private static class ModuleReference {
        public String prefix;
        public String beanIdentifier;

        ModuleReference(String prefix, String beanIdentifier) {
            this.prefix = prefix;
            this.beanIdentifier = beanIdentifier;
        }
    }
}
