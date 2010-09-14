package org.maera.plugin;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;

public class MockModuleDescriptor<T> extends AbstractModuleDescriptor<T> {
    private T module;
    private String key;
    private Plugin plugin;

    public MockModuleDescriptor(Plugin plugin, String key, T module) {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
        this.module = module;
        this.plugin = plugin;
        this.key = key;
    }

    public T getModule() {
        return module;
    }

    @Override
    public String getCompleteKey() {
        return plugin.getKey() + ":" + key;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public Class<T> getModuleClass() {
        return (Class<T>) module.getClass();
    }

    @Override
    protected void loadClass(Plugin plugin, String clazz) throws PluginParseException {
        // no need since we override getModuleClass
    }
}
