package org.maera.plugin.module;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.PluginParseException;

/**
 * The {@link ModuleFactory} creates the module class of a {@link org.maera.plugin.ModuleDescriptor}.
 * The ModuleFactory is injected into the {@link org.maera.plugin.descriptors.AbstractModuleDescriptor} and encapsulates the different
 * strategies how the module class can be created.
 *
 * @since 2.5.0
 */
public interface ModuleFactory {
    /**
     * Creates the module instance. The module class name can contain a prefix. The delimiter of the prefix and the class name is ':'.
     * E.g.: 'bean:httpServletBean'. Which prefixes are supported depends on the registered {@link org.maera.plugin.module.ModuleCreator}.
     * The prefix is case in-sensitive.
     *
     * @param name             module class name, can contain a prefix followed by ":" and the class name. Cannot be null
     *                         If no prefix provided a default behaviour is assumed how to create the module class.
     * @param moduleDescriptor the {@link org.maera.plugin.ModuleDescriptor}. Cannot be null
     * @return an instantiated object of the module class.
     * @throws PluginParseException If it failed to create the object.
     */
    <T> T createModule(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException;

    /**
     * Returns the module class. The module class name can contain a prefix. The delimiter of the prefix and the class name is ':'.
     * E.g.: 'bean:httpServletBean'. Which prefixes are supported depends on the registered {@link org.maera.plugin.module.ModuleCreator}.
     *
     * @param name               module class name, can contain a prefix followed by ":" and the class name. Cannot be null.
     * If no prefix provided a default behaviour is assumed how to create the module class.
     * @param moduleDescriptor   the {@link org.maera.plugin.ModuleDescriptor}. Cannot be null
     * @return the module class.
     * @throws ModuleClassNotFoundException If the module class could not be found
     */
    //<T> Class<T> getModuleClass(String name, ModuleDescriptor<T> moduleDescriptor) throws ModuleClassNotFoundException;

    ModuleFactory LEGACY_MODULE_FACTORY = new LegacyModuleFactory();
}
