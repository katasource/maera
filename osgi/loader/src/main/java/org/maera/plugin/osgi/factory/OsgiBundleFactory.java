package org.maera.plugin.osgi.factory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.maera.plugin.*;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.factories.PluginFactory;
import org.maera.plugin.impl.UnloadablePlugin;
import org.maera.plugin.loaders.classloading.DeploymentUnit;
import org.maera.plugin.osgi.container.OsgiContainerException;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.util.OsgiHeaderUtil;
import org.maera.plugin.parsers.DescriptorParser;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * Plugin deployer that deploys OSGi bundles that don't contain XML descriptor files
 */
public class OsgiBundleFactory implements PluginFactory {
    private static final Logger log = LoggerFactory.getLogger(OsgiBundleFactory.class);

    private final OsgiContainerManager osgi;
    private final PluginEventManager pluginEventManager;
    private final OsgiPluginXmlDescriptorParserFactory descriptorParserFactory;
    private final String pluginDescriptorFileName;

    public OsgiBundleFactory(OsgiContainerManager osgi, PluginEventManager pluginEventManager) {
        this(PluginAccessor.Descriptor.FILENAME, osgi, pluginEventManager);
    }

    public OsgiBundleFactory(String pluginDescriptorFileName, OsgiContainerManager osgi, PluginEventManager pluginEventManager) {
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        Validate.notNull(osgi, "The osgi container is required");
        Validate.notNull(pluginEventManager, "The plugin event manager is required");
        this.osgi = osgi;
        this.pluginEventManager = pluginEventManager;
        this.descriptorParserFactory = new OsgiPluginXmlDescriptorParserFactory();
    }

    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");
        String pluginKey = null;
        InputStream manifestStream = null;
        InputStream descriptorStream = null;

        try {
            manifestStream = pluginArtifact.getResourceAsStream("META-INF/MANIFEST.MF");
            if (manifestStream != null) {
                Manifest mf;
                try {
                    mf = new Manifest(manifestStream);
                } catch (IOException e) {
                    throw new PluginParseException("Unable to parse manifest", e);
                }
                String symName = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                if (symName != null) {
                    pluginKey = OsgiHeaderUtil.getPluginKey(mf);

                    // Check for a descriptor in case it really is a version 1 plugin
                    descriptorStream = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);
                    if (descriptorStream != null) {
                        final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream);
                        if (descriptorParser.getPluginsVersion() == 1) {
                            // Nope, it isn't a bundle
                            pluginKey = null;
                        }
                    }

                }
            }
            return pluginKey;
        }
        finally {
            IOUtils.closeQuietly(manifestStream);
            IOUtils.closeQuietly(descriptorStream);
        }
    }

    /**
     * @deprecated Since 2.2.0, use {@link #create(PluginArtifact,ModuleDescriptorFactory)} instead
     */
    public Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        Validate.notNull(deploymentUnit, "The deployment unit is required");
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
        Validate.notNull(pluginArtifact, "The plugin artifact is required");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory is required");

        File file = pluginArtifact.toFile();
        Bundle bundle;
        try {
            bundle = osgi.installBundle(file);
        } catch (OsgiContainerException ex) {
            return reportUnloadablePlugin(file, ex);
        }
        String key = OsgiHeaderUtil.getPluginKey(bundle);
        return new OsgiBundlePlugin(bundle, key, pluginEventManager);
    }

    private Plugin reportUnloadablePlugin(File file, Exception e) {
        log.error("Unable to load plugin: " + file, e);

        UnloadablePlugin plugin = new UnloadablePlugin();
        plugin.setErrorText("Unable to load plugin: " + e.getMessage());
        return plugin;
    }
}