package org.maera.plugin.impl;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.apache.commons.lang.StringUtils;
import org.maera.plugin.*;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.util.VersionStringComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractPlugin implements Plugin, Comparable<Plugin> {
    private final Map<String, ModuleDescriptor<?>> modules = CopyOnWriteMap.<String, ModuleDescriptor<?>>builder().stableViews().newLinkedMap();
    private String name;
    private String i18nNameKey;
    private String key;
    private boolean enabledByDefault = true;
    private PluginInformation pluginInformation = new PluginInformation();
    private boolean system;
    private Resourced resources = Resources.EMPTY_RESOURCES;
    private int pluginsVersion = 1;
    private final Date dateLoaded = new Date();
    private final AtomicReference<PluginState> pluginState = new AtomicReference<PluginState>(PluginState.UNINSTALLED);

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public String getName() {
        return !StringUtils.isBlank(name) ? name : !StringUtils.isBlank(i18nNameKey) ? "" : getKey();
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the logger used internally
     */
    protected Logger getLog() {
        return log;
    }

    public String getI18nNameKey() {
        return i18nNameKey;
    }

    public void setI18nNameKey(final String i18nNameKey) {
        this.i18nNameKey = i18nNameKey;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String aPackage) {
        key = aPackage;
    }

    public void addModuleDescriptor(final ModuleDescriptor<?> moduleDescriptor) {
        modules.put(moduleDescriptor.getKey(), moduleDescriptor);
    }

    protected void removeModuleDescriptor(final String key) {
        modules.remove(key);
    }

    /**
     * Returns the module descriptors for this plugin
     *
     * @return An unmodifiable list of the module descriptors.
     */
    public Collection<ModuleDescriptor<?>> getModuleDescriptors() {
        return modules.values();
    }

    public ModuleDescriptor<?> getModuleDescriptor(final String key) {
        return modules.get(key);
    }

    public <T> List<ModuleDescriptor<T>> getModuleDescriptorsByModuleClass(final Class<T> aClass) {
        final List<ModuleDescriptor<T>> result = new ArrayList<ModuleDescriptor<T>>();
        for (final ModuleDescriptor<?> moduleDescriptor : modules.values()) {
            final Class<?> moduleClass = moduleDescriptor.getModuleClass();
            if (aClass.isAssignableFrom(moduleClass)) {
                @SuppressWarnings("unchecked")
                final ModuleDescriptor<T> typedModuleDescriptor = (ModuleDescriptor<T>) moduleDescriptor;
                result.add(typedModuleDescriptor);
            }
        }
        return result;
    }

    public PluginState getPluginState() {
        return pluginState.get();
    }

    protected void setPluginState(final PluginState state) {
        if (log.isDebugEnabled()) {
            log.debug("Plugin " + getKey() + " going from " + getPluginState() + " to " + state);
        }
        pluginState.set(state);
    }

    /**
     * Only sets the plugin state if it is in the expected state.
     *
     * @param requiredExistingState The expected state
     * @param desiredState          The desired state
     * @return True if the set was successful, false if not in the expected
     *         state
     * @since 2.4
     */
    protected boolean compareAndSetPluginState(final PluginState requiredExistingState, final PluginState desiredState) {
        if (log.isDebugEnabled()) {
            log.debug("Plugin " + getKey() + " trying to go from " + getPluginState() + " to " + desiredState + " but only if in " + requiredExistingState);
        }
        return pluginState.compareAndSet(requiredExistingState, desiredState);
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault && ((pluginInformation == null) || pluginInformation.satisfiesMinJavaVersion());
    }

    public void setEnabledByDefault(final boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public int getPluginsVersion() {
        return pluginsVersion;
    }

    public void setPluginsVersion(final int pluginsVersion) {
        this.pluginsVersion = pluginsVersion;
    }

    public PluginInformation getPluginInformation() {
        return pluginInformation;
    }

    public void setPluginInformation(final PluginInformation pluginInformation) {
        this.pluginInformation = pluginInformation;
    }

    public void setResources(final Resourced resources) {
        this.resources = resources != null ? resources : Resources.EMPTY_RESOURCES;
    }

    public List<ResourceDescriptor> getResourceDescriptors() {
        return resources.getResourceDescriptors();
    }

    public List<ResourceDescriptor> getResourceDescriptors(final String type) {
        return resources.getResourceDescriptors(type);
    }

    public ResourceLocation getResourceLocation(final String type, final String name) {
        return resources.getResourceLocation(type, name);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public ResourceDescriptor getResourceDescriptor(final String type, final String name) {
        return resources.getResourceDescriptor(type, name);
    }

    /**
     * @return true if the plugin has been enabled
     */
    @Deprecated
    public boolean isEnabled() {
        return getPluginState() == PluginState.ENABLED;
    }

    public final void enable() {
        final PluginState state = pluginState.get();
        if ((state == PluginState.ENABLED) || (state == PluginState.ENABLING)) {
            return;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Enabling plugin '" + getKey() + "'");
        }
        try {
            // not ideal as comparison and set doesn't happen atomically
            final PluginState desiredState = enableInternal();
            if ((desiredState != PluginState.ENABLED) && (desiredState != PluginState.ENABLING)) {
                log.warn("Illegal state transition to " + desiredState + " for plugin '" + getKey() + "' on enable()");
            }
            setPluginState(desiredState);
        }
        catch (final PluginException ex) {
            log.warn("Unable to enable plugin '" + getKey() + "'", ex);
            throw ex;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Enabled plugin '" + getKey() + "'");
        }
    }

    /**
     * Perform any internal enabling logic. Subclasses should only throw
     * {@link PluginException}.
     *
     * @return Either {@link PluginState#ENABLED} or
     *         {@link PluginState#ENABLING}
     * @throws PluginException If the plugin could not be enabled
     * @since 2.2.0
     */
    protected PluginState enableInternal() throws PluginException {
        return PluginState.ENABLED;
    }

    public final void disable() {
        if (pluginState.get() == PluginState.DISABLED) {
            return;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Disabling plugin '" + getKey() + "'");
        }
        try {
            disableInternal();
            setPluginState(PluginState.DISABLED);
        }
        catch (final PluginException ex) {
            log.warn("Unable to disable plugin '" + getKey() + "'", ex);
            throw ex;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Disabled plugin '" + getKey() + "'");
        }
    }

    /**
     * Perform any internal disabling logic. Subclasses should only throw
     * {@link PluginException}.
     *
     * @throws PluginException If the plugin could not be disabled
     * @since 2.2.0
     */
    protected void disableInternal() throws PluginException {
    }

    public Set<String> getRequiredPlugins() {
        return Collections.emptySet();
    }

    public void close() {
        uninstall();
    }

    public final void install() {
        if (pluginState.get() == PluginState.INSTALLED) {
            return;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Installing plugin '" + getKey() + "'");
        }
        try {
            installInternal();
            setPluginState(PluginState.INSTALLED);
        }
        catch (final PluginException ex) {
            log.warn("Unable to install plugin '" + getKey() + "'", ex);
            throw ex;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Installed plugin '" + getKey() + "'");
        }
    }

    /**
     * Perform any internal installation logic. Subclasses should only throw
     * {@link PluginException}.
     *
     * @throws PluginException If the plugin could not be installed
     * @since 2.2.0
     */
    protected void installInternal() throws PluginException {
    }

    public final void uninstall() {
        if (pluginState.get() == PluginState.UNINSTALLED) {
            return;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Uninstalling plugin '" + getKey() + "'");
        }
        try {
            uninstallInternal();
            setPluginState(PluginState.UNINSTALLED);
        }
        catch (final PluginException ex) {
            log.warn("Unable to uninstall plugin '" + getKey() + "'", ex);
            throw ex;
        }
        if (getLog().isDebugEnabled()) {
            getLog().debug("Uninstalled plugin '" + getKey() + "'");
        }
    }

    /**
     * Perform any internal uninstallation logic. Subclasses should only throw
     * {@link PluginException}.
     *
     * @throws PluginException If the plugin could not be uninstalled
     * @since 2.2.0
     */
    protected void uninstallInternal() throws PluginException {
    }

    /**
     * Setter for the enabled state of a plugin. If this is set to false then
     * the plugin will not execute.
     */
    @Deprecated
    public void setEnabled(final boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }

    public boolean isSystemPlugin() {
        return system;
    }

    public boolean containsSystemModule() {
        for (final ModuleDescriptor<?> moduleDescriptor : modules.values()) {
            if (moduleDescriptor.isSystemModule()) {
                return true;
            }
        }
        return false;
    }

    public void setSystemPlugin(final boolean system) {
        this.system = system;
    }

    public Date getDateLoaded() {
        return dateLoaded;
    }

    public boolean isBundledPlugin() {
        return false;
    }

    /**
     * Compares this Plugin to another Plugin for order. The primary sort field
     * is the key, and the secondary field is the version number.
     *
     * @param otherPlugin The plugin to be compared.
     * @return a negative integer, zero, or a positive integer as this Plugin is
     *         less than, equal to, or greater than the specified Plugin.
     * @see VersionStringComparator
     * @see Comparable#compareTo
     */
    public int compareTo(final Plugin otherPlugin) {
        if (otherPlugin.getKey() == null) {
            if (getKey() == null) {
                // both null keys - not going to bother checking the version,
                // who cares?
                return 0;
            }
            return 1;
        }
        if (getKey() == null) {
            return -1;
        }

        // If the compared plugin doesn't have the same key, the current object
        // is greater
        if (!otherPlugin.getKey().equals(getKey())) {
            return getKey().compareTo(otherPlugin.getKey());
        }

        final String thisVersion = cleanVersionString((getPluginInformation() != null ? getPluginInformation().getVersion() : null));
        final String otherVersion = cleanVersionString((otherPlugin.getPluginInformation() != null ? otherPlugin.getPluginInformation().getVersion() : null));

        // Valid versions should come after invalid versions because when we
        // find multiple instances of a plugin, we choose the "latest".
        if (!VersionStringComparator.isValidVersionString(thisVersion)) {
            if (!VersionStringComparator.isValidVersionString(otherVersion)) {
                // both invalid
                return 0;
            }
            return -1;
        }
        if (!VersionStringComparator.isValidVersionString(otherVersion)) {
            return 1;
        }

        return new VersionStringComparator().compare(thisVersion, otherVersion);
    }

    private String cleanVersionString(final String version) {
        if ((version == null) || version.trim().equals("")) {
            return "0";
        }
        return version.replaceAll(" ", "");
    }

    @Override
    public String toString() {
        final PluginInformation info = getPluginInformation();
        return getKey() + ":" + (info == null ? "?" : info.getVersion());
    }
}
