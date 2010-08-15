package org.maera.plugin.descriptors;

import org.dom4j.Element;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;

/**
 * Utility class to create UnloadableModuleDescriptor instances when there are problems
 */
public final class UnloadableModuleDescriptorFactory {
    /**
     * Creates a new UnloadableModuleDescriptor, for when a problem occurs during the construction
     * of the ModuleDescriptor itself.
     * <p/>
     * This instance has the same information as the original ModuleDescriptor, but also contains
     * an error message that reports the error.
     *
     * @param plugin                  the Plugin the ModuleDescriptor belongs to
     * @param element                 the XML Element used to construct the ModuleDescriptor
     * @param e                       the Throwable
     * @param moduleDescriptorFactory a ModuleDescriptorFactory used to retrieve ModuleDescriptor instances
     * @return a new UnloadableModuleDescriptor instance
     * @throws PluginParseException if there was a problem constructing the UnloadableModuleDescriptor
     */
    public static UnloadableModuleDescriptor createUnloadableModuleDescriptor(final Plugin plugin, final Element element, final Throwable e, final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        final UnloadableModuleDescriptor descriptor = new UnloadableModuleDescriptor();
        descriptor.init(plugin, element);

        final String name = element.getName();
        final Class<? extends ModuleDescriptor> descriptorClass = moduleDescriptorFactory.getModuleDescriptorClass(name);
        String descriptorClassName;

        if (descriptorClass == null) {
            descriptorClassName = descriptor.getKey();
        } else {
            descriptorClassName = descriptorClass.getName();
        }

        final String errorMsg = constructErrorMessage(plugin, name, descriptorClassName, e);

        descriptor.setErrorText(errorMsg);

        return descriptor;
    }

    /**
     * Creates a new UnloadableModuleDescriptor based on an existing ModuleDescriptor, descriptor
     * <p/>
     * This method uses the information in an existing descriptor to construct a new UnloadableModuleDescriptor.
     *
     * @param plugin     the Plugin the ModuleDescriptor belongs to
     * @param descriptor the ModuleDescriptor that reported an error
     * @param e          the Throwable
     * @return a new UnloadableModuleDescriptor instance
     */
    public static UnloadableModuleDescriptor createUnloadableModuleDescriptor(final Plugin plugin, final ModuleDescriptor<?> descriptor, final Throwable e) {
        final UnloadableModuleDescriptor unloadableDescriptor = new UnloadableModuleDescriptor();
        unloadableDescriptor.setName(descriptor.getName());
        unloadableDescriptor.setKey(descriptor.getKey());
        unloadableDescriptor.setPlugin(plugin);

        final String errorMsg = constructErrorMessage(plugin, descriptor.getName(),
                (descriptor.getModuleClass() == null ? descriptor.getName() : descriptor.getModuleClass().getName()), e);

        unloadableDescriptor.setErrorText(errorMsg);

        return unloadableDescriptor;
    }

    /**
     * Constructs an error message from a module and exception
     *
     * @param plugin      the Plugin the module belongs to
     * @param moduleName  the name of the module
     * @param moduleClass the class of the module
     * @param e           the Throwable
     * @return an appropriate String representing the error
     */
    private static String constructErrorMessage(final Plugin plugin, final String moduleName, final String moduleClass, final Throwable e) {
        String errorMsg;

        if (e instanceof PluginParseException) {
            errorMsg = "There was a problem loading the descriptor for module '" + moduleName + "' in plugin '" + (plugin == null ? "null" : plugin.getName()) + "'.\n ";
        } else if (e instanceof InstantiationException) {
            errorMsg = "Could not instantiate module descriptor: " + moduleClass + ".<br/>";
        } else if (e instanceof IllegalAccessException) {
            errorMsg = "Exception instantiating module descriptor: " + moduleClass + ".<br/>";
        } else if (e instanceof ClassNotFoundException) {
            errorMsg = "Could not find module descriptor class: " + moduleClass + ".<br/>";
        } else if (e instanceof NoClassDefFoundError) {
            errorMsg = "A required class was missing: " + moduleClass + ". Please check that you have all of the required dependencies.<br/>";
        } else {
            errorMsg = "There was a problem loading the module descriptor: " + moduleClass + ".<br/>";
        }

        return errorMsg + e.getMessage();
    }
}
