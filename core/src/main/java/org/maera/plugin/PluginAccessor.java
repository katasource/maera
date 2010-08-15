package org.maera.plugin;

import org.maera.plugin.predicate.ModuleDescriptorPredicate;
import org.maera.plugin.predicate.PluginPredicate;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * Allows access to the current plugin system state
 */
public interface PluginAccessor {
    /**
     * The plugin descriptor file.
     *
     * @since 2.2
     */
    public static final class Descriptor {
        /**
         * The default filename.
         */
        public static final String FILENAME = "atlassian-plugin.xml";

        private Descriptor() {
        }
    }

    /**
     * Gets all of the currently installed plugins.
     *
     * @return a collection of installed {@link Plugin}s.
     */
    Collection<Plugin> getPlugins();

    /**
     * Gets all installed plugins that match the given predicate.
     *
     * @param pluginPredicate the {@link PluginPredicate} to match.
     * @return a collection of {@link Plugin}s that match the given predicate.
     * @since 0.17
     */
    Collection<Plugin> getPlugins(final PluginPredicate pluginPredicate);

    /**
     * Get all of the currently enabled plugins.
     *
     * @return a collection of installed and enabled {@link Plugin}s.
     */
    Collection<Plugin> getEnabledPlugins();

    /**
     * Gets all installed modules that match the given predicate.
     *
     * @param moduleDescriptorPredicate the {@link org.maera.plugin.predicate.ModuleDescriptorPredicate} to match.
     * @return a collection of modules as per {@link ModuleDescriptor#getModule()} that match the given predicate.
     * @since 0.17
     */
    <M> Collection<M> getModules(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate);

    /**
     * Gets all module descriptors of installed modules that match the given predicate.
     *
     * @param moduleDescriptorPredicate the {@link org.maera.plugin.predicate.ModuleDescriptorPredicate} to match.
     * @return a collection of {@link ModuleDescriptor}s that match the given predicate.
     * @since 0.17
     */
    <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(final ModuleDescriptorPredicate<M> moduleDescriptorPredicate);

    /**
     * Retrieve a given plugin (whether enabled or not).
     *
     * @param key The plugin key.  Cannot be null.
     * @return The enabled plugin, or null if that plugin does not exist.
     * @throws IllegalArgumentException If the plugin key is null
     */
    Plugin getPlugin(String key) throws IllegalArgumentException;

    /**
     * Retrieve a given plugin if it is enabled.
     *
     * @return The enabled plugin, or null if that plugin does not exist or is disabled.
     * @throws IllegalArgumentException If the plugin key is null
     */
    Plugin getEnabledPlugin(String pluginKey) throws IllegalArgumentException;

    /**
     * Retrieve any plugin module by complete module key.
     * <p/>
     * Note: the module may or may not be disabled.
     */
    ModuleDescriptor<?> getPluginModule(String completeKey);

    /**
     * Retrieve an enabled plugin module by complete module key.
     */
    ModuleDescriptor<?> getEnabledPluginModule(String completeKey);

    /**
     * Whether or not a given plugin is currently enabled.
     *
     * @throws IllegalArgumentException If the plugin key is null
     */
    boolean isPluginEnabled(String key) throws IllegalArgumentException;

    /**
     * Whether or not a given plugin module is currently enabled.  This also checks
     * if the plugin it is contained within is enabled also
     *
     * @see #isPluginEnabled(String)
     */
    boolean isPluginModuleEnabled(String completeKey);

    /**
     * Retrieve all plugin modules that implement or extend a specific class.
     *
     * @return List of modules that implement or extend the given class.
     */
    <M> List<M> getEnabledModulesByClass(Class<M> moduleClass);

    /**
     * Retrieve all plugin modules that implement or extend a specific class, and has a descriptor class
     * as one of descriptorClazz
     *
     * @param descriptorClazz @NotNull
     * @param moduleClass     @NotNull
     * @return List of modules that implement or extend the given class. Empty list if none found
     * @deprecated since 0.17, use {@link #getModules(org.maera.plugin.predicate.ModuleDescriptorPredicate)} with an appropriate predicate instead.
     */
    @Deprecated
    <M> List<M> getEnabledModulesByClassAndDescriptor(Class<ModuleDescriptor<M>>[] descriptorClazz, Class<M> moduleClass);

    /**
     * Retrieve all plugin modules that implement or extend a specific class, and has a descriptor class
     * as the descriptorClazz
     *
     * @param descriptorClass @NotNull
     * @param moduleClass     @NotNull
     * @return List of modules that implement or extend the given class. Empty list if none found
     * @deprecated since 0.17, use {@link #getModules(org.maera.plugin.predicate.ModuleDescriptorPredicate)} with an appropriate predicate instead.
     */
    @Deprecated
    <M> List<M> getEnabledModulesByClassAndDescriptor(final Class<ModuleDescriptor<M>> descriptorClass, final Class<M> moduleClass);

    /**
     * Get all enabled module descriptors that have a specific descriptor class.
     *
     * @param descriptorClazz module descriptor class
     * @return List of {@link ModuleDescriptor}s that implement or extend the given class.
     */
    <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(Class<D> descriptorClazz);

    /**
     * Get all enabled module descriptors that have a specific descriptor class.
     *
     * @param descriptorClazz module descriptor class
     * @param verbose         log verbose messages flag
     * @return List of {@link ModuleDescriptor}s that implement or extend the given class.
     * @deprecated Since 2.3.0, use {@link #getEnabledModuleDescriptorsByClass(Class<D>)} instead
     */
    <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(Class<D> descriptorClazz, boolean verbose);

    /**
     * Get all enabled module descriptors that have a specific descriptor type.
     *
     * @return List of {@link ModuleDescriptor}s that are of a given type.
     * @deprecated since 0.17, use {@link #getModuleDescriptors(org.maera.plugin.predicate.ModuleDescriptorPredicate)} with an appropriate predicate instead.
     */
    @Deprecated
    <M> List<ModuleDescriptor<M>> getEnabledModuleDescriptorsByType(String type) throws PluginParseException;

    /**
     * Retrieve a resource from a currently loaded (and active) dynamically loaded plugin. Will return the first resource
     * found, so plugins with overlapping resource names will behave eratically.
     *
     * @param resourcePath the path to the resource to retrieve
     * @return the dynamically loaded resource that matches that path, or null if no such resource is found
     */
    InputStream getDynamicResourceAsStream(String resourcePath);

    /**
     * Retrieve a resource from a currently loaded (and active) plugin. For statically loaded plugins, this just means
     * pulling the resource from the PluginManager's classloader. For dynamically loaded plugins, this means retrieving
     * the resource from the plugin's private classloader.
     *
     * @deprecated since 0.21 this method is not used, use
     *             {@link #getPlugin(String)}.{@link Plugin#getClassLoader() getClassLoader()}.{@link ClassLoader#getResourceAsStream(String) getResourceAsStream(String)}
     */
    @Deprecated
    InputStream getPluginResourceAsStream(String pluginKey, String resourcePath);

    /**
     * Retrieve a class from a currently loaded (and active) dynamically loaded plugin. Will return the first class
     * found, so plugins with overlapping class names will behave eratically.
     *
     * @param className the name of the class to retrieve
     * @return the dynamically loaded class that matches that name
     * @throws ClassNotFoundException thrown if no classes by that name could be found in any of the enabled dynamic plugins
     * @deprecated since 0.21 this method is not used, use
     *             {@link #getPlugin(String)}.{@link Plugin#getClassLoader() getClassLoader()}.{@link ClassLoader#loadClass(String) loadClass(String)}
     */
    @Deprecated
    Class<?> getDynamicPluginClass(String className) throws ClassNotFoundException;

    /**
     * Retrieve the class loader responsible for loading classes and resources from plugins.
     *
     * @return the class loader
     * @since 0.21
     */
    ClassLoader getClassLoader();

    /**
     * @return true if the plugin is a system plugin.
     */
    boolean isSystemPlugin(String key);

    /**
     * Gets the state of the plugin upon restart.  Only useful for plugins that contain module descriptors with the
     * \@RestartRequired annotation, and therefore, cannot be dynamically installed, upgraded, or removed at runtime
     *
     * @param key The plugin key
     * @return The state of the plugin on restart
     */
    PluginRestartState getPluginRestartState(String key);
}
