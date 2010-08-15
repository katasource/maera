package org.maera.plugin;

import org.maera.plugin.predicate.ModuleDescriptorPredicate;
import org.maera.plugin.predicate.PluginPredicate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 2.3.0
 */
public class MockPluginAccessor implements PluginAccessor {
    private Collection<Plugin> allPlugins = new ArrayList<Plugin>();

    public Collection<Plugin> getPlugins() {
        return allPlugins;
    }

    public void addPlugin(Plugin plugin) {
        allPlugins.add(plugin);
    }

    public Collection<Plugin> getPlugins(final PluginPredicate pluginPredicate) {
        throw new UnsupportedOperationException();
    }

    public Collection<Plugin> getEnabledPlugins() {
        Collection<Plugin> enabledPlugins = new ArrayList<Plugin>();
        for (Plugin plugin : allPlugins) {
            if (plugin.isEnabled()) {
                enabledPlugins.add(plugin);
            }
        }
        return enabledPlugins;
    }

    public <M> Collection<M> getModules(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate) {
        throw new UnsupportedOperationException();
    }

    public <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate) {
        throw new UnsupportedOperationException();
    }

    public Plugin getPlugin(final String key) {
        for (Plugin plugin : allPlugins) {
            if (key.equals(plugin.getKey())) {
                return plugin;
            }
        }
        return null;
    }

    public Plugin getEnabledPlugin(final String pluginKey) {
        throw new UnsupportedOperationException();
    }

    public ModuleDescriptor<?> getPluginModule(final String completeKey) {
        throw new UnsupportedOperationException();
    }

    public ModuleDescriptor<?> getEnabledPluginModule(final String completeKey) {
        throw new UnsupportedOperationException();
    }

    public boolean isPluginEnabled(final String key) {
        return getPlugin(key).isEnabled();
    }

    public boolean isPluginModuleEnabled(final String completeKey) {
        return false;
    }

    public <M> List<M> getEnabledModulesByClass(final Class<M> moduleClass) {
        throw new UnsupportedOperationException();
    }

    public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>>[] descriptorClazz, final Class<M> moduleClass) {
        throw new UnsupportedOperationException();
    }

    public <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>> descriptorClass, final Class<M> moduleClass) {
        throw new UnsupportedOperationException();
    }

    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz) {
        throw new UnsupportedOperationException();
    }

    public <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(final Class<D> descriptorClazz, final boolean verbose) {
        throw new UnsupportedOperationException();
    }

    public <M> List<ModuleDescriptor<M>> getEnabledModuleDescriptorsByType(final String type)
            throws PluginParseException {
        throw new UnsupportedOperationException();
    }

    public InputStream getDynamicResourceAsStream(final String resourcePath) {
        throw new UnsupportedOperationException();
    }

    public InputStream getPluginResourceAsStream(final String pluginKey, final String resourcePath) {
        throw new UnsupportedOperationException();
    }

    public Class<?> getDynamicPluginClass(final String className) throws ClassNotFoundException {
        throw new UnsupportedOperationException();
    }

    public ClassLoader getClassLoader() {
        throw new UnsupportedOperationException();
    }

    public boolean isSystemPlugin(final String key) {
        return false;
    }

    public PluginRestartState getPluginRestartState(final String key) {
        throw new UnsupportedOperationException();
    }
}
