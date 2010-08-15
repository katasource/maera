package org.maera.plugin.factories;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.dom4j.DocumentException;
import org.maera.plugin.*;
import org.maera.plugin.impl.XmlDynamicPlugin;
import org.maera.plugin.loaders.classloading.DeploymentUnit;
import org.maera.plugin.parsers.DescriptorParser;
import org.maera.plugin.parsers.DescriptorParserFactory;
import org.maera.plugin.parsers.XmlDescriptorParserFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Deploys plugins that consist of an XML descriptor file.
 *
 * @since 2.1.0
 */
public class XmlDynamicPluginFactory implements PluginFactory {
    private final DescriptorParserFactory descriptorParserFactory;
    private final Set<String> applicationKeys;

    /**
     * @deprecated Since 2.2.0, use {@link XmlDynamicPluginFactory(String)} instead
     */
    @Deprecated
    public XmlDynamicPluginFactory() {
        this(Collections.<String>emptySet());
    }

    /**
     * @param applicationKey The application key to use to choose modules
     * @since 2.2.0
     */
    public XmlDynamicPluginFactory(final String applicationKey) {
        this(new HashSet<String>(Arrays.asList(applicationKey)));
    }

    /**
     * @param applicationKeys The application key to use to choose modules
     * @since 2.2.0
     */
    public XmlDynamicPluginFactory(final Set<String> applicationKeys) {
        descriptorParserFactory = new XmlDescriptorParserFactory();
        Validate.notNull(applicationKeys, "applicationKeys");
        this.applicationKeys = applicationKeys;
    }

    /**
     * @deprecated Since 2.2.0, use {@link #create(PluginArtifact,ModuleDescriptorFactory)} instead
     */
    @Deprecated
    public Plugin create(final DeploymentUnit deploymentUnit, final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        return create(new XmlPluginArtifact(deploymentUnit.getPath()), moduleDescriptorFactory);
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
    public Plugin create(final PluginArtifact pluginArtifact, final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact must not be null");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory must not be null");

        InputStream pluginDescriptor = null;
        try {
            pluginDescriptor = new FileInputStream(pluginArtifact.toFile());
            // The plugin we get back may not be the same (in the case of an UnloadablePlugin), so add what gets returned, rather than the original
            final DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor,
                    applicationKeys.toArray(new String[applicationKeys.size()]));
            return parser.configurePlugin(moduleDescriptorFactory, new XmlDynamicPlugin());
        }
        catch (final RuntimeException e) {
            throw new PluginParseException(e);
        }
        catch (final IOException e) {
            throw new PluginParseException();
        }
        finally {
            IOUtils.closeQuietly(pluginDescriptor);
        }
    }

    /**
     * Determines if this deployer can handle this artifact by looking for the plugin descriptor
     *
     * @param pluginArtifact The artifact to test
     * @return The plugin key, null if it cannot load the plugin
     * @throws org.maera.plugin.PluginParseException
     *          If there are exceptions parsing the plugin configuration
     */
    public String canCreate(final PluginArtifact pluginArtifact) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact must not be null");
        InputStream descriptorStream = null;
        try {
            descriptorStream = pluginArtifact.getInputStream();
            if (descriptorStream == null) {
                return null;
            }
            final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream,
                    applicationKeys.toArray(new String[applicationKeys.size()]));
            return descriptorParser.getKey();
        }
        catch (final PluginParseException ex) {
            if (!(ex.getCause() instanceof DocumentException)) {
                throw ex;
            }
            return null;
        }
        finally {
            IOUtils.closeQuietly(descriptorStream);
        }
    }
}