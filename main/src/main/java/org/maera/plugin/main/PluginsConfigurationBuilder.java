package org.maera.plugin.main;

import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.manager.PluginPersistentStateStore;
import org.maera.plugin.manager.store.MemoryPluginPersistentStateStore;
import org.maera.plugin.osgi.container.OsgiPersistentCache;
import org.maera.plugin.osgi.container.PackageScannerConfiguration;
import org.maera.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.maera.plugin.util.Assertions.isTrue;
import static org.maera.plugin.util.Assertions.notNull;

/**
 * The builder for {@link PluginsConfiguration} instances that additionally performs validation and default creation.
 * For a usage example, see the package javadocs.
 * <p/>
 * Not thread-safe. Instances of this class should be thread and preferably method local.
 *
 * @since 2.2
 */
public class PluginsConfigurationBuilder {
    /**
     * Static factory for creating a new builder.
     *
     * @return a new builder.
     */
    public static PluginsConfigurationBuilder pluginsConfiguration() {
        return new PluginsConfigurationBuilder();
    }

    private PackageScannerConfiguration packageScannerConfiguration;
    private HostComponentProvider hostComponentProvider;
    private File osgiPersistentCache;
    private File pluginDirectory;
    private URL bundledPluginUrl;
    private File bundledPluginCacheDirectory;
    private String pluginDescriptorFilename;
    private ModuleDescriptorFactory moduleDescriptorFactory;
    private PluginPersistentStateStore pluginStateStore;
    private long hotDeployPollingPeriod;
    private boolean useLegacyDynamicPluginDeployer = false;
    private String applicationKey;

    /**
     * Sets the package scanner configuration instance that contains information about what packages to expose to plugins.
     *
     * @param packageScannerConfiguration The configuration instance
     * @return this
     */
    public PluginsConfigurationBuilder packageScannerConfiguration(final PackageScannerConfiguration packageScannerConfiguration) {
        this.packageScannerConfiguration = notNull("packageScannerConfiguration", packageScannerConfiguration);
        return this;
    }

    /**
     * Sets the host component provider instance, used for registering application services as OSGi services so that
     * they can be automatically available to plugins
     *
     * @param hostComponentProvider The host component provider implementation
     * @return this
     */
    public PluginsConfigurationBuilder hostComponentProvider(final HostComponentProvider hostComponentProvider) {
        this.hostComponentProvider = notNull("hostComponentProvider", hostComponentProvider);
        return this;
    }

    /**
     * Sets the set of persistent caching directories the OSGi classes will use.  This includes the caching directory
     * to extract framework bundles into, the container bundle cache, and the transformed plugin cache.
     *
     * @param osgiPersistentCache A directory that exists
     * @return this
     */
    public PluginsConfigurationBuilder osgiPersistentCache(final File osgiPersistentCache) {
        this.osgiPersistentCache = osgiPersistentCache;
        return this;
    }

    /**
     * Sets the directory that contains the plugins and will be used to store installed plugins.
     *
     * @param pluginDirectory A directory that exists
     * @return this
     */
    public PluginsConfigurationBuilder pluginDirectory(final File pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
        return this;
    }

    /**
     * Sets the URL to a ZIP file containing plugins that are to be started before any user plugins but after
     * framework bundles.  Must be set if {@link #bundledPluginCacheDirectory(java.io.File)} is set.
     *
     * @param bundledPluginUrl A URL to a ZIP of plugin JAR files
     * @return this
     */
    public PluginsConfigurationBuilder bundledPluginUrl(final URL bundledPluginUrl) {
        this.bundledPluginUrl = bundledPluginUrl;
        return this;
    }

    /**
     * Sets the directory to unzip bundled plugins into.  The directory will automatically be cleaned out if the
     * framework detects any modification.  Must be set if {@link #bundledPluginUrl(java.net.URL)} is set.
     *
     * @param bundledPluginCacheDirectory A directory that exists
     * @return this
     */
    public PluginsConfigurationBuilder bundledPluginCacheDirectory(final File bundledPluginCacheDirectory) {
        this.bundledPluginCacheDirectory = bundledPluginCacheDirectory;
        return this;
    }

    /**
     * Sets the plugin descriptor file name to expect in a plugin JAR artifact
     *
     * @param pluginDescriptorFilename A valid file name
     * @return this
     */
    public PluginsConfigurationBuilder pluginDescriptorFilename(final String pluginDescriptorFilename) {
        this.pluginDescriptorFilename = pluginDescriptorFilename;
        return this;
    }

    /**
     * Sets the module descriptor factory that will be used to create instances of discovered module descriptors.
     * Usually, the {@link DefaultModuleDescriptorFactory} is what is used, which takes class instances of module
     * descriptors to instantiate.
     *
     * @param moduleDescriptorFactory A module descriptor factory instance
     * @return this
     */
    public PluginsConfigurationBuilder moduleDescriptorFactory(final ModuleDescriptorFactory moduleDescriptorFactory) {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        return this;
    }

    /**
     * Sets the plugin state store implementation used for persisting which plugins and modules are enabled or disabled
     * across restarts.
     *
     * @param pluginStateStore The plugin state store implementation
     * @return this
     */
    public PluginsConfigurationBuilder pluginStateStore(final PluginPersistentStateStore pluginStateStore) {
        this.pluginStateStore = pluginStateStore;
        return this;
    }

    /**
     * Sets the polling frequency for scanning for new plugins
     *
     * @param hotDeployPollingFrequency The quantity of time periods
     * @param timeUnit                  The units for the frequency
     * @return {@code this}
     */
    public PluginsConfigurationBuilder hotDeployPollingFrequency(final long hotDeployPollingFrequency, final TimeUnit timeUnit) {
        hotDeployPollingPeriod = timeUnit.toMillis(hotDeployPollingFrequency);
        return this;
    }

    /**
     * Defines whether ther legacy plugin deployer should be used or not.
     *
     * @param useLegacyDynamicPluginDeployer {@code true} if the legacy plugin deployer should be used.
     * @return {@code this}
     */
    public PluginsConfigurationBuilder useLegacyDynamicPluginDeployer(final boolean useLegacyDynamicPluginDeployer) {
        this.useLegacyDynamicPluginDeployer = useLegacyDynamicPluginDeployer;
        return this;
    }

    /**
     * Sets the current application key to use when filtering modules in a plugin
     *
     * @param applicationKey the current application identifier
     * @return {@code this}
     */
    public PluginsConfigurationBuilder applicationKey(final String applicationKey) {
        this.applicationKey = applicationKey;
        return this;
    }

    /**
     * Builds a {@link org.maera.plugin.main.PluginsConfiguration} instance by processing the configuration that
     * was previously set, validating the input, and setting any defaults where not explicitly specified.
     *
     * @return A valid {@link PluginsConfiguration} instance to pass to {@link AtlassianPlugins}
     */
    public PluginsConfiguration build() {
        notNull("Plugin directory must be defined", pluginDirectory);
        isTrue("Plugin directory must exist", pluginDirectory.exists());

        if (packageScannerConfiguration == null) {
            packageScannerConfiguration = new PackageScannerConfigurationBuilder().build();
        }
        if (pluginDescriptorFilename == null) {
            pluginDescriptorFilename = PluginAccessor.Descriptor.FILENAME;
        }

        if (hostComponentProvider == null) {
            hostComponentProvider = new HostComponentProvider() {
                public void provide(final ComponentRegistrar registrar) {
                }
            };
        }

        if (pluginStateStore == null) {
            pluginStateStore = new MemoryPluginPersistentStateStore();
        }

        if (moduleDescriptorFactory == null) {
            moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        }

        if (osgiPersistentCache == null) {
            osgiPersistentCache = createTempDir("atlassian-plugins-osgi-cache");
        }

        if (bundledPluginUrl != null && bundledPluginCacheDirectory == null) {
            throw new IllegalArgumentException("Bundled plugin cache directory MUST be defined when bundled plugin URL is defined");
        }

        return new InternalPluginsConfiguration(this);
    }

    private File createTempDir(final String prefix) {
        try {
            final File directory = File.createTempFile(prefix, AtlassianPlugins.TEMP_DIRECTORY_SUFFIX);
            directory.delete();
            directory.mkdir();
            return directory;
        }
        catch (final IOException e) {
            throw new IllegalStateException("Was not able to create temp file with prefix <" + prefix + ">", e);
        }
    }

    private static class InternalPluginsConfiguration implements PluginsConfiguration {
        private final PackageScannerConfiguration packageScannerConfiguration;
        private final HostComponentProvider hostComponentProvider;
        private final OsgiPersistentCache osgiPersistentCache;
        private final File pluginDirectory;
        private final URL bundledPluginUrl;
        private final File bundledPluginCacheDirectory;
        private final String pluginDescriptorFilename;
        private final ModuleDescriptorFactory moduleDescriptorFactory;
        private final PluginPersistentStateStore pluginStateStore;
        private final long hotDeployPollingPeriod;
        private final boolean useLegacyDynamicPluginDeployer;
        private final String applicationKey;

        InternalPluginsConfiguration(final PluginsConfigurationBuilder builder) {
            packageScannerConfiguration = builder.packageScannerConfiguration;
            hostComponentProvider = builder.hostComponentProvider;
            osgiPersistentCache = new DefaultOsgiPersistentCache(builder.osgiPersistentCache, builder.packageScannerConfiguration.getCurrentHostVersion());
            pluginDirectory = builder.pluginDirectory;
            bundledPluginUrl = builder.bundledPluginUrl;
            bundledPluginCacheDirectory = builder.bundledPluginCacheDirectory;
            pluginDescriptorFilename = builder.pluginDescriptorFilename;
            moduleDescriptorFactory = builder.moduleDescriptorFactory;
            pluginStateStore = builder.pluginStateStore;
            hotDeployPollingPeriod = builder.hotDeployPollingPeriod;
            useLegacyDynamicPluginDeployer = builder.useLegacyDynamicPluginDeployer;
            applicationKey = builder.applicationKey;
        }

        public PackageScannerConfiguration getPackageScannerConfiguration() {
            return packageScannerConfiguration;
        }

        public HostComponentProvider getHostComponentProvider() {
            return hostComponentProvider;
        }

        public String getPluginDescriptorFilename() {
            return pluginDescriptorFilename;
        }

        public File getPluginDirectory() {
            return pluginDirectory;
        }

        public URL getBundledPluginUrl() {
            return bundledPluginUrl;
        }

        public File getBundledPluginCacheDirectory() {
            return bundledPluginCacheDirectory;
        }

        public ModuleDescriptorFactory getModuleDescriptorFactory() {
            return moduleDescriptorFactory;
        }

        public PluginPersistentStateStore getPluginStateStore() {
            return pluginStateStore;
        }

        public long getHotDeployPollingPeriod() {
            return hotDeployPollingPeriod;
        }

        public boolean isUseLegacyDynamicPluginDeployer() {
            return useLegacyDynamicPluginDeployer;
        }

        public String getApplicationKey() {
            return applicationKey;
        }

        public OsgiPersistentCache getOsgiPersistentCache() {
            return osgiPersistentCache;
        }
    }
}
