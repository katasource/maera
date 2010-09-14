package org.maera.plugin.impl;

import org.apache.commons.lang.Validate;
import org.maera.plugin.*;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Delegating plugin that supports easy wrapping
 *
 * @since 2.2.0
 */
public abstract class AbstractDelegatingPlugin implements Plugin, Comparable<Plugin>, AutowireCapablePlugin {
    private final Plugin delegate;

    public AbstractDelegatingPlugin(final Plugin delegate) {
        Validate.notNull(delegate);
        this.delegate = delegate;
    }

    public int getPluginsVersion() {
        return delegate.getPluginsVersion();
    }

    public void setPluginsVersion(final int version) {
        delegate.setPluginsVersion(version);
    }

    public String getName() {
        return delegate.getName();
    }

    public void setName(final String name) {
        delegate.setName(name);
    }

    public String getI18nNameKey() {
        return delegate.getI18nNameKey();
    }

    public void setI18nNameKey(final String i18nNameKey) {
        delegate.setI18nNameKey(i18nNameKey);
    }

    public String getKey() {
        return delegate.getKey();
    }

    public void setKey(final String aPackage) {
        delegate.setKey(aPackage);
    }

    public void addModuleDescriptor(final ModuleDescriptor<?> moduleDescriptor) {
        delegate.addModuleDescriptor(moduleDescriptor);
    }

    public Collection<ModuleDescriptor<?>> getModuleDescriptors() {
        return delegate.getModuleDescriptors();
    }

    public ModuleDescriptor<?> getModuleDescriptor(final String key) {
        return delegate.getModuleDescriptor(key);
    }

    public <M> List<ModuleDescriptor<M>> getModuleDescriptorsByModuleClass(final Class<M> moduleClass) {
        return delegate.getModuleDescriptorsByModuleClass(moduleClass);
    }

    public boolean isEnabledByDefault() {
        return delegate.isEnabledByDefault();
    }

    public void setEnabledByDefault(final boolean enabledByDefault) {
        delegate.setEnabledByDefault(enabledByDefault);
    }

    public PluginInformation getPluginInformation() {
        return delegate.getPluginInformation();
    }

    public void setPluginInformation(final PluginInformation pluginInformation) {
        delegate.setPluginInformation(pluginInformation);
    }

    public void setResources(final Resourced resources) {
        delegate.setResources(resources);
    }

    public PluginState getPluginState() {
        return delegate.getPluginState();
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public boolean isSystemPlugin() {
        return delegate.isSystemPlugin();
    }

    public boolean containsSystemModule() {
        return delegate.containsSystemModule();
    }

    public void setSystemPlugin(final boolean system) {
        delegate.setSystemPlugin(system);
    }

    public boolean isBundledPlugin() {
        return delegate.isBundledPlugin();
    }

    public Date getDateLoaded() {
        return delegate.getDateLoaded();
    }

    public boolean isUninstallable() {
        return delegate.isUninstallable();
    }

    public boolean isDeleteable() {
        return delegate.isDeleteable();
    }

    public boolean isDynamicallyLoaded() {
        return delegate.isDynamicallyLoaded();
    }

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException {
        return delegate.loadClass(clazz, callingClass);
    }

    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }

    public URL getResource(final String path) {
        return delegate.getResource(path);
    }

    public InputStream getResourceAsStream(final String name) {
        return delegate.getResourceAsStream(name);
    }

    public void setEnabled(final boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public void close() {
        delegate.close();
    }

    public void install() {
        delegate.install();
    }

    public void uninstall() {
        delegate.uninstall();
    }

    public void enable() {
        delegate.enable();
    }

    public void disable() {
        delegate.disable();
    }

    public Set<String> getRequiredPlugins() {
        return delegate.getRequiredPlugins();
    }

    public List<ResourceDescriptor> getResourceDescriptors() {
        return delegate.getResourceDescriptors();
    }

    public List<ResourceDescriptor> getResourceDescriptors(final String type) {
        return delegate.getResourceDescriptors(type);
    }

    public ResourceDescriptor getResourceDescriptor(final String type, final String name) {
        return delegate.getResourceDescriptor(type, name);
    }

    public ResourceLocation getResourceLocation(final String type, final String name) {
        return delegate.getResourceLocation(type, name);
    }

    public int compareTo(final Plugin o) {
        return delegate.compareTo(o);
    }

    public Plugin getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return delegate.equals(obj);
    }

    /**
     * @throws UnsupportedOperationException If the underlying delegate doesn't
     *                                       implement {@link AutowireCapablePlugin}
     * @since 2.3.0
     */
    public <T> T autowire(final Class<T> clazz) throws UnsupportedOperationException {
        if (delegate instanceof AutowireCapablePlugin) {
            return ((AutowireCapablePlugin) delegate).autowire(clazz);
        } else {
            throw new UnsupportedOperationException("The AutowireCapablePlugin interface is not implemented by the " + "delegate '" + delegate.getClass().getSimpleName() + "'");
        }
    }

    /**
     * @throws UnsupportedOperationException If the underlying delegate doesn't
     *                                       implement {@link AutowireCapablePlugin}
     * @since 2.3.0
     */
    public <T> T autowire(final Class<T> clazz, final AutowireStrategy autowireStrategy) throws UnsupportedOperationException {
        if (delegate instanceof AutowireCapablePlugin) {
            return ((AutowireCapablePlugin) delegate).autowire(clazz, autowireStrategy);
        } else {
            throw new UnsupportedOperationException("The AutowireCapablePlugin interface is not implemented by the " + "delegate '" + delegate.getClass().getSimpleName() + "'");
        }
    }

    /**
     * @throws UnsupportedOperationException If the underlying delegate doesn't
     *                                       implement {@link AutowireCapablePlugin}
     * @since 2.3.0
     */
    public void autowire(final Object instance) throws UnsupportedOperationException {
        if (delegate instanceof AutowireCapablePlugin) {
            ((AutowireCapablePlugin) delegate).autowire(instance);
        } else {
            throw new UnsupportedOperationException("The AutowireCapablePlugin interface is not implemented by the " + "delegate '" + delegate.getClass().getSimpleName() + "'");
        }
    }

    /**
     * @throws UnsupportedOperationException If the underlying delegate doesn't
     *                                       implement {@link AutowireCapablePlugin}
     * @since 2.3.0
     */
    public void autowire(final Object instance, final AutowireStrategy autowireStrategy) throws UnsupportedOperationException {
        if (delegate instanceof AutowireCapablePlugin) {
            ((AutowireCapablePlugin) delegate).autowire(instance, autowireStrategy);
        } else {
            throw new UnsupportedOperationException("The AutowireCapablePlugin interface is not implemented by the " + "delegate '" + delegate.getClass().getSimpleName() + "'");
        }
    }
}