package org.maera.plugin.factories;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.maera.plugin.*;
import org.maera.plugin.classloader.PluginClassLoader;
import org.maera.plugin.impl.DefaultDynamicPlugin;
import org.maera.plugin.loaders.classloading.DeploymentUnit;
import org.maera.plugin.parsers.DescriptorParser;
import org.maera.plugin.parsers.DescriptorParserFactory;
import org.maera.plugin.parsers.XmlDescriptorParserFactory;

import java.io.File;
import java.io.InputStream;

/**
 * Deploys version 1.0 plugins into the legacy custom classloader structure that gives each plugin its own classloader.
 *
 * @since 2.0.0
 */
public class LegacyDynamicPluginFactory implements PluginFactory {
    private final DescriptorParserFactory descriptorParserFactory;
    private final String pluginDescriptorFileName;
    private final File tempDirectory;

    public LegacyDynamicPluginFactory(String pluginDescriptorFileName) {
        this(pluginDescriptorFileName, new File(System.getProperty("java.io.tmpdir")));
    }

    public LegacyDynamicPluginFactory(String pluginDescriptorFileName, File tempDirectory) {
        this.tempDirectory = tempDirectory;
        Validate.notEmpty(pluginDescriptorFileName, "Plugin descriptor name cannot be null or blank");
        this.descriptorParserFactory = new XmlDescriptorParserFactory();
        this.pluginDescriptorFileName = pluginDescriptorFileName;
    }

    /**
     * @deprecated Since 2.2.0, use {@link #create(PluginArtifact,ModuleDescriptorFactory)} instead
     */
    public Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        return create(new JarPluginArtifact(deploymentUnit.getPath()), moduleDescriptorFactory);
    }

    /**
     * Deploys the plugin artifact
     *
     * @param pluginArtifact          the plugin artifact to deploy
     * @param moduleDescriptorFactory The factory for plugin modules
     * @return The instantiated and populated plugin
     * @throws PluginParseException If the descriptor cannot be parsed
     * @since 2.2.0
     */
    public Plugin create(PluginArtifact pluginArtifact, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The deployment unit must not be null");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory must not be null");

        File file = pluginArtifact.toFile();
        Plugin plugin = null;
        InputStream pluginDescriptor = null;
        PluginClassLoader loader = null;
        try {
            pluginDescriptor = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);
            if (pluginDescriptor == null)
                throw new PluginParseException("No descriptor found in classloader for : " + file);

            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor);
            loader = new PluginClassLoader(file, Thread.currentThread().getContextClassLoader(), tempDirectory);
            plugin = parser.configurePlugin(moduleDescriptorFactory, createPlugin(pluginArtifact, loader));
        }
        // Under normal conditions, the deployer would be closed when the plugins are undeployed. However,
        // these are not normal conditions, so we need to make sure that we close them explicitly.
        catch (PluginParseException e) {
            if (loader != null) loader.close();
            throw e;
        }
        catch (RuntimeException e) {
            if (loader != null) loader.close();
            throw new PluginParseException(e);
        }
        catch (Error e) {
            if (loader != null) loader.close();
            throw e;
        } finally {
            IOUtils.closeQuietly(pluginDescriptor);
        }
        return plugin;
    }

    /**
     * @deprecated Since 2.2.0, use {@link #createPlugin(PluginArtifact,PluginClassLoader)} instead
     */
    protected Plugin createPlugin(DeploymentUnit deploymentUnit, PluginClassLoader loader) {
        return createPlugin(new JarPluginArtifact(deploymentUnit.getPath()), loader);
    }

    /**
     * Creates the plugin.  Override to use a different Plugin class
     *
     * @param pluginArtifact The plugin artifact
     * @param loader         The plugin loader
     * @return The plugin instance
     * @since 2.2.0
     */
    protected Plugin createPlugin(PluginArtifact pluginArtifact, PluginClassLoader loader) {
        return new DefaultDynamicPlugin(pluginArtifact, loader);
    }

    /**
     * Determines if this deployer can handle this artifact by looking for the plugin descriptor
     *
     * @param pluginArtifact The artifact to test
     * @return The plugin key, null if it cannot load the plugin
     * @throws org.maera.plugin.PluginParseException
     *          If there are exceptions parsing the plugin configuration
     */
    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact must not be null");
        String pluginKey = null;
        InputStream descriptorStream = null;
        try {
            descriptorStream = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);
            if (descriptorStream != null) {
                final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);

                // Only recognize version 1 plugins
                if (descriptorParser.getPluginsVersion() <= 1)
                    pluginKey = descriptorParser.getKey();
            }
        } finally {
            IOUtils.closeQuietly(descriptorStream);
        }
        return pluginKey;
    }
}
