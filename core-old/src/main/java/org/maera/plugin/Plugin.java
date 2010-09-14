package org.maera.plugin;

import java.io.InputStream;
import java.net.URL;
import java.util.*;

public interface Plugin extends Resourced, Comparable<Plugin> {
    /**
     * @deprecated since 2.2.0.  This comparator only takes into account the plugin name and assumes it is not null,
     *             yet a) that constraint is not validated anywhere in plugin loading and b) the plugin could have used the i18n
     *             name, and only the application can resolve that to a name useful for comparisons.
     */
    public static final Comparator<Plugin> NAME_COMPARATOR = new PluginNameComparator();

    /**
     * Gets the version of the plugins system to handle this plugin
     *
     * @return The plugins version.  If undefined, assumed to be 1.
     */
    int getPluginsVersion();

    /**
     * Sets the version of the plugins system
     *
     * @param version The version
     */
    void setPluginsVersion(int version);

    /**
     * Returns the non-localised name of this plugin if defined.
     * <p/>
     * <p> This corresponds to the value of the {@code name} field in the plugin's XML configuration file.
     * <p/>
     * <p> You would expect a plugin developer to fill in one of either {@code name}, or {@code i18n-name-key},
     * but the framework does no validation and makes no guarantees that this is the case.
     *
     * @return the non-localised name of this plugin if defined, or null.
     * @see #getI18nNameKey()
     */
    String getName();

    /**
     * Sets the non-localised name of this plugin.
     *
     * @param name the name.
     * @see #getName()
     */
    void setName(String name);

    /**
     * Returns the i18nKey used to get an internationalised name for this plugin.
     * <p/>
     * <p> This corresponds to the value of the {@code i18n-name-key} field in the plugin's XML configuration file.
     * <p/>
     * <p> You would expect a plugin developer to fill in one of either {@code name}, or {@code i18n-name-key},
     * but the framework does no validation and makes no guarantees that this is the case.
     *
     * @return the i18n Name Key for this plugin if defined, or null.
     * @see #getName()
     */
    String getI18nNameKey();

    /**
     * Sets the i18nKey used to get an internationalised name for this plugin.
     *
     * @param i18nNameKey the i18n Name Key.
     * @see #getI18nNameKey()
     */
    void setI18nNameKey(String i18nNameKey);

    String getKey();

    void setKey(String aPackage);

    void addModuleDescriptor(ModuleDescriptor<?> moduleDescriptor);

    /**
     * Get the {@link Collection} of {@link ModuleDescriptor descriptors}.
     * <p/>
     * <p> The iteration order of the collection is
     * the order that the modules will be enabled, and should be the same order that the modules appear in the
     * plugin descriptor.
     *
     * @return the modules contained by this plugin in the order they are to be enabled
     */
    Collection<ModuleDescriptor<?>> getModuleDescriptors();

    /**
     * Get the {@link ModuleDescriptor} for a particular key. Returns <tt>null</tt> if the plugin does not exist.
     * <p/>
     * Note: The {@link ModuleDescriptor#getModule()} may throw {@link ClassCastException} if the expected type is incorrect.
     *
     * @param key the {@link String} complete key of the module, in the form "org.example.plugin:module-key".
     * @return the {@link ModuleDescriptor} of the expected type.
     */
    ModuleDescriptor<?> getModuleDescriptor(String key);

    /**
     * Get the {@link ModuleDescriptor descriptors} whose module class implements or is assignable from the supplied {@link Class}.
     * <p/>
     * Note: The {@link ModuleDescriptor#getModule()} may throw {@link ClassCastException} if the expected type is incorrect.
     * Normally this method would not be supplied with anything other than {@link Object} or &lt;?&gt;, unless you are
     * confident in the super type of the module classes this {@link Plugin} provides.
     *
     * @param <M>         The expected module type of the returned {@link ModuleDescriptor descriptors}.
     * @param moduleClass the {@link Class super class} the {@link ModuleDescriptor descriptors} return.
     * @return the {@link List} of {@link ModuleDescriptor descriptors} of the expected type.
     */
    <M> List<ModuleDescriptor<M>> getModuleDescriptorsByModuleClass(Class<M> moduleClass);

    boolean isEnabledByDefault();

    void setEnabledByDefault(boolean enabledByDefault);

    PluginInformation getPluginInformation();

    void setPluginInformation(PluginInformation pluginInformation);

    void setResources(Resourced resources);

    /**
     * Returns this plugin's current state.
     *
     * @return the current state of the plugin.
     * @since 2.2.0
     */
    PluginState getPluginState();

    /**
     * @return {@code true} if this plugin is enabled.
     * @deprecated since 2.2.0, use {@link #getPluginState()} instead
     */
    boolean isEnabled();


    /**
     * Whether the plugin is a "system" plugin that shouldn't be made visible to the user.
     *
     * @return {@code true} if this plugin is a "system" plugin.
     */
    boolean isSystemPlugin();

    void setSystemPlugin(boolean system);

    boolean containsSystemModule();

    /**
     * Whether the plugin is a "bundled" plugin that can't be removed.
     *
     * @return {@code true} if this plugin is a "bundled" plugin.
     */
    boolean isBundledPlugin();

    /**
     * The date this plugin was loaded into the system.
     *
     * @return The date this plugin was loaded into the system.
     */
    Date getDateLoaded();

    /**
     * Whether or not this plugin can be 'uninstalled'.
     *
     * @return {@code true} if this plugin can be 'uninstalled'.
     */
    boolean isUninstallable();

    /**
     * Should the plugin file be deleted on unistall?
     *
     * @return {@code true} if this plugin file should be deleted on unistall.
     */
    boolean isDeleteable();

    /**
     * Whether or not this plugin is loaded dynamically at runtime.
     *
     * @return {@code true} if this plugin is loaded dynamically at runtime.
     */
    boolean isDynamicallyLoaded();

    /**
     * Get the plugin to load a specific class.
     *
     * @param clazz        The name of the class to be loaded
     * @param callingClass The class calling the loading (used to help find a classloader)
     * @return The loaded class.
     * @throws ClassNotFoundException if the class cannot be located.
     */
    <T> Class<T> loadClass(String clazz, Class<?> callingClass) throws ClassNotFoundException;

    /**
     * Get the classloader for the plugin.
     *
     * @return The classloader used to load classes for this plugin
     */
    ClassLoader getClassLoader();

    /**
     * Retrieve the URL of the resource from the plugin.
     *
     * @param path the name of the resource to be loaded
     * @return The URL to the resource, or null if the resource is not found
     */
    URL getResource(String path);

    /**
     * Load a given resource from the plugin. Plugins that are loaded dynamically will need
     * to implement this in a way that loads the resource from the same context as the plugin.
     * Static plugins can just pull them from their own classloader.
     *
     * @param name The name of the resource to be loaded.
     * @return An InputStream for the resource, or null if the resource is not found.
     */
    InputStream getResourceAsStream(String name);

    /**
     * @param enabled new enabled state
     * @deprecated Since 2.2.0, use {@link #enable()} or {@link #disable()} instead
     */
    void setEnabled(boolean enabled);

    /**
     * Free any resources held by this plugin.  To be called during uninstallation of the {@link Plugin}.
     *
     * @deprecated Since 2.2.0, use {@link #uninstall()} instead
     */
    void close();

    /**
     * Installs the plugin into any internal, managing container.  This method will be called on every startup.  Unless
     * an exception is thrown, the plugin should be in the {@link PluginState#INSTALLED} state.  If the plugin is already
     * in the {@link PluginState#INSTALLED} state, nothing will happen.
     *
     * @throws PluginException If the plugin could not be installed
     * @since 2.2.0
     */
    void install() throws PluginException;

    /**
     * Uninstalls the plugin from any internal container.  This method will be called on every shutdown.  Unless an
     * exception is thrown, the plugin should be in the {@link PluginState#UNINSTALLED} state.  If the plugin is already
     * in the {@link PluginState#UNINSTALLED} state, nothing will happen.
     *
     * @throws PluginException If the plugin could not be uninstalled
     * @since 2.2.0
     */
    void uninstall() throws PluginException;

    /**
     * Enables the plugin.  Unless an exception is thrown, the plugin should then be in either the
     * {@link PluginState#ENABLING} or {@link PluginState#ENABLED} state.  If the plugin is already in the
     * {@link PluginState#ENABLING} or {@link PluginState#ENABLED} state, nothing will happen.
     *
     * @throws PluginException If the plugin could not be enabled
     * @since 2.2.0
     */
    void enable() throws PluginException;

    /**
     * Disables the plugin.  Unless an exception is thrown, the plugin should be in the {@link PluginState#DISABLED}
     * state. If the plugin is already in the {@link PluginState#DISABLED} state, nothing will happen.
     *
     * @throws PluginException If the plugin could not be disabled
     * @since 2.2.0 If the plugin could not be disabled
     */
    void disable() throws PluginException;

    /**
     * @return A list of plugin keys that this plugin is dependent upon, or an empty list if none
     * @since 2.2.0
     */
    Set<String> getRequiredPlugins();
}
