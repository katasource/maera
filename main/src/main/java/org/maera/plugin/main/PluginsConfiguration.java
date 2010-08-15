package org.maera.plugin.main;

import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.manager.PluginPersistentStateStore;
import org.maera.plugin.osgi.container.OsgiPersistentCache;
import org.maera.plugin.osgi.container.PackageScannerConfiguration;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;

import java.io.File;
import java.net.URL;

/**
 * Configuration for the Atlassian Plugins Framework.  Instances of this class should be created via the
 * {@link PluginsConfigurationBuilder}.
 */
public interface PluginsConfiguration {
    /**
     * @return The package scanner configuration
     */
    PackageScannerConfiguration getPackageScannerConfiguration();

    /**
     * @return the host component provider
     */
    HostComponentProvider getHostComponentProvider();

    /**
     * @return the persistent cache configuration
     */
    OsgiPersistentCache getOsgiPersistentCache();

    /**
     * @return the name of the plugin descriptor file
     */
    String getPluginDescriptorFilename();

    /**
     * @return the directory containing plugins
     */
    File getPluginDirectory();

    /**
     * @return the location of the bundled plugins zip
     */
    URL getBundledPluginUrl();

    /**
     * @return the directory to unzip the bundled plugins into
     */
    File getBundledPluginCacheDirectory();

    /**
     * @return the factory for module descriptors
     */
    ModuleDescriptorFactory getModuleDescriptorFactory();

    /**
     * @return the plugin state store implementation
     */
    PluginPersistentStateStore getPluginStateStore();

    /**
     * @return the number of milliseconds between polling.  Zero to disable.
     */
    long getHotDeployPollingPeriod();

    /**
     * @return whether to use the legacy plugin deployer or not.
     */
    boolean isUseLegacyDynamicPluginDeployer();

    /**
     * @return the application key to use to filter modules in the descriptor
     */
    String getApplicationKey();
}
