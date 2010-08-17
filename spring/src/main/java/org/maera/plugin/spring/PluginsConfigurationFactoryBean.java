package org.maera.plugin.spring;

import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.main.PluginsConfiguration;
import org.maera.plugin.main.PluginsConfigurationBuilder;
import org.maera.plugin.osgi.container.PackageScannerConfiguration;
import org.maera.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Spring {@link org.springframework.beans.factory.FactoryBean FactoryBean} implementation that creates a
 * {@link PluginsConfiguration} instance at startup based on Spring configuration.
 *
 * @since 0.1
 */
public class PluginsConfigurationFactoryBean extends AbstractFactoryBean/*<PluginsConfiguration>*/ {

    private static final transient Logger log = LoggerFactory.getLogger(PluginsConfigurationFactoryBean.class);

    private HostComponentProvider hostComponentProvider;
    private long hotDeployPollingMillis;
    private ModuleDescriptorFactory moduleDescriptorFactory;
    private PackageScannerConfiguration packageScannerConfiguration;
    private Resource pluginDirectory;

    public PluginsConfigurationFactoryBean() {
        this.hotDeployPollingMillis = 2000;
        this.packageScannerConfiguration = new DefaultPackageScannerConfiguration();
    }

    @Override
    public Class<?> getObjectType() {
        return PluginsConfiguration.class;
    }

    @Override
    protected PluginsConfiguration createInstance() throws Exception {
        if (this.pluginDirectory == null) {
            String msg = "pluginDirectory property must be configured.";
            throw new BeanInitializationException(msg);
        }
        File pluginDir;
        log.trace("Resolving pluginDirectory {}", this.pluginDirectory);
        try {
            pluginDir = this.pluginDirectory.getFile();
        } catch (IOException e) {
            String msg = "Unable to locate pluginDirectory [" + this.pluginDirectory + "].  This " +
                    "_must_ point to an existing filesystem directory.";
            throw new BeanInitializationException(msg, e);
        }

        if (pluginDir.exists()) {
            log.trace("Plugin directory exists.  Checking to see if it is a directory and not a file.");
            if (!pluginDir.isDirectory()) {
                String msg = "pluginDirectory property [" + this.pluginDirectory + "] exists, but it is not a " +
                        "directory.  This property must be a directory and not a file.";
                throw new BeanInitializationException(msg);
            }
            log.debug("Found existing plugin directory {}", this.pluginDirectory);
        } else {
            log.debug("Plugin directory {} does not exist.  Creating...", this.pluginDirectory);
            boolean created = false;
            Exception exception = null;
            try {
                created = pluginDir.mkdirs();
            } catch (Exception e) {
                //save for the possible rethrow below
                exception = e;
            }

            if (!created) {
                String msg = "Unable to lazily create specified pluginDirectory [" + this.pluginDirectory + "].  This " +
                        "path must either exist as a directory or be able to be created at startup.  Please ensure the " +
                        "path format is correct as well as that any parent directory (or directories) have the correct " +
                        "file permissions to allow this directory to be created.";
                throw new BeanInitializationException(msg, exception);
            }

            log.debug("Successfully created plugin directory {}", this.pluginDirectory);
        }

        PluginsConfigurationBuilder builder = new PluginsConfigurationBuilder()
                .pluginDirectory(pluginDir)
                .packageScannerConfiguration(this.packageScannerConfiguration)
                .hotDeployPollingFrequency(this.hotDeployPollingMillis, TimeUnit.MILLISECONDS);

        if (this.hostComponentProvider != null) {
            builder.hostComponentProvider(this.hostComponentProvider);
        }

        if (this.moduleDescriptorFactory != null) {
            builder.moduleDescriptorFactory(this.moduleDescriptorFactory);
        }

        return builder.build();
    }

    public HostComponentProvider getHostComponentProvider() {
        return hostComponentProvider;
    }

    public void setHostComponentProvider(HostComponentProvider hostComponentProvider) {
        this.hostComponentProvider = hostComponentProvider;
    }

    public long getHotDeployPollingMillis() {
        return hotDeployPollingMillis;
    }

    public void setHotDeployPollingMillis(long hotDeployPollingMillis) {
        this.hotDeployPollingMillis = hotDeployPollingMillis;
    }

    public ModuleDescriptorFactory getModuleDescriptorFactory() {
        return moduleDescriptorFactory;
    }

    public void setModuleDescriptorFactory(ModuleDescriptorFactory moduleDescriptorFactory) {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
    }

    public PackageScannerConfiguration getPackageScannerConfiguration() {
        return packageScannerConfiguration;
    }

    public void setPackageScannerConfiguration(PackageScannerConfiguration packageScannerConfiguration) {
        this.packageScannerConfiguration = packageScannerConfiguration;
    }

    public Resource getPluginDirectory() {
        return pluginDirectory;
    }

    public void setPluginDirectory(Resource pluginDirectory) {
        this.pluginDirectory = pluginDirectory;
    }
}
