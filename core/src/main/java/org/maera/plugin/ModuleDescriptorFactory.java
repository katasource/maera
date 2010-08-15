package org.maera.plugin;

public interface ModuleDescriptorFactory {
    ModuleDescriptor<?> getModuleDescriptor(String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException;

    Class<? extends ModuleDescriptor> getModuleDescriptorClass(String type);

    boolean hasModuleDescriptor(String type);
}
