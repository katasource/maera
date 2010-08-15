package org.maera.plugin.osgi.factory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.maera.plugin.*;
import org.maera.plugin.descriptors.ChainModuleDescriptorFactory;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.factories.PluginFactory;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.impl.UnloadablePlugin;
import org.maera.plugin.loaders.classloading.DeploymentUnit;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.container.OsgiPersistentCache;
import org.maera.plugin.osgi.external.ListableModuleDescriptorFactory;
import org.maera.plugin.osgi.factory.descriptor.ComponentImportModuleDescriptor;
import org.maera.plugin.osgi.factory.descriptor.ComponentModuleDescriptor;
import org.maera.plugin.osgi.factory.descriptor.ModuleTypeModuleDescriptor;
import org.maera.plugin.osgi.factory.transform.DefaultPluginTransformer;
import org.maera.plugin.osgi.factory.transform.PluginTransformationException;
import org.maera.plugin.osgi.factory.transform.PluginTransformer;
import org.maera.plugin.osgi.factory.transform.model.SystemExports;
import org.maera.plugin.parsers.DescriptorParser;
import org.maera.plugin.parsers.DescriptorParserFactory;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;

/**
 * Plugin loader that starts an OSGi container and loads plugins into it, wrapped as OSGi bundles.
 */
public class OsgiPluginFactory implements PluginFactory {
    private static final Logger log = LoggerFactory.getLogger(OsgiPluginFactory.class);

    private final OsgiContainerManager osgi;
    private final String pluginDescriptorFileName;
    private final DescriptorParserFactory descriptorParserFactory;
    private final PluginEventManager pluginEventManager;
    private final Set<String> applicationKeys;
    private final OsgiPersistentCache persistentCache;
    private final ModuleDescriptorFactory transformedDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer()) {{
        addModuleDescriptor("component", ComponentModuleDescriptor.class);
        addModuleDescriptor("component-import", ComponentImportModuleDescriptor.class);
        addModuleDescriptor("module-type", ModuleTypeModuleDescriptor.class);
    }};

    private volatile PluginTransformer pluginTransformer;

    private ServiceTracker moduleDescriptorFactoryTracker;

    public OsgiPluginFactory(String pluginDescriptorFileName, String applicationKey, OsgiPersistentCache persistentCache, OsgiContainerManager osgi, PluginEventManager pluginEventManager) {
        this(pluginDescriptorFileName, new HashSet<String>(Arrays.asList(applicationKey)), persistentCache, osgi, pluginEventManager);
    }

    public OsgiPluginFactory(String pluginDescriptorFileName, Set<String> applicationKeys, OsgiPersistentCache persistentCache, OsgiContainerManager osgi, PluginEventManager pluginEventManager) {
        Validate.notNull(pluginDescriptorFileName, "Plugin descriptor is required");
        Validate.notNull(osgi, "The OSGi container is required");
        Validate.notNull(applicationKeys, "The application keys are required");
        Validate.notNull(persistentCache, "The osgi persistent cache is required");
        Validate.notNull(persistentCache, "The plugin event manager is required");

        this.osgi = osgi;
        this.pluginDescriptorFileName = pluginDescriptorFileName;
        this.descriptorParserFactory = new OsgiPluginXmlDescriptorParserFactory();
        this.pluginEventManager = pluginEventManager;
        this.applicationKeys = applicationKeys;
        this.persistentCache = persistentCache;
    }

    private PluginTransformer getPluginTransformer() {
        if (pluginTransformer == null) {
            String exportString = (String) osgi.getBundles()[0].getHeaders()
                    .get(Constants.EXPORT_PACKAGE);
            SystemExports exports = new SystemExports(exportString);
            pluginTransformer = new DefaultPluginTransformer(persistentCache, exports, applicationKeys, pluginDescriptorFileName, osgi);
        }
        return pluginTransformer;
    }

    public String canCreate(PluginArtifact pluginArtifact) throws PluginParseException {
        Validate.notNull(pluginArtifact, "The plugin artifact is required");

        String pluginKey = null;
        InputStream descriptorStream = null;
        try {
            descriptorStream = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);

            if (descriptorStream != null) {
                final DescriptorParser descriptorParser = descriptorParserFactory.getInstance(descriptorStream, applicationKeys.toArray(new String[applicationKeys.size()]));
                if (descriptorParser.getPluginsVersion() == 2) {
                    pluginKey = descriptorParser.getKey();
                }
            }
        }
        finally {
            IOUtils.closeQuietly(descriptorStream);
        }
        return pluginKey;
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
        Validate.notNull(pluginArtifact, "The plugin deployment unit is required");
        Validate.notNull(moduleDescriptorFactory, "The module descriptor factory is required");

        Plugin plugin = null;
        InputStream pluginDescriptor = null;
        try {
            pluginDescriptor = pluginArtifact.getResourceAsStream(pluginDescriptorFileName);
            if (pluginDescriptor == null) {
                throw new PluginParseException("No descriptor found in classloader for : " + pluginArtifact);
            }

            ModuleDescriptorFactory combinedFactory = getChainedModuleDescriptorFactory(moduleDescriptorFactory, pluginArtifact);
            DescriptorParser parser = descriptorParserFactory.getInstance(pluginDescriptor, applicationKeys.toArray(new String[applicationKeys.size()]));

            final Plugin osgiPlugin = new OsgiPlugin(parser.getKey(), osgi, createOsgiPluginJar(pluginArtifact), pluginEventManager);

            // Temporarily configure plugin until it can be properly installed
            plugin = parser.configurePlugin(combinedFactory, osgiPlugin);
        }
        catch (PluginTransformationException ex) {
            return reportUnloadablePlugin(pluginArtifact.toFile(), ex);
        }
        finally {
            IOUtils.closeQuietly(pluginDescriptor);
        }
        return plugin;
    }

    /**
     * Get a chained module descriptor factory that includes any dynamically available descriptor factories
     *
     * @param originalFactory The factory provided by the host application
     * @param pluginArtifact
     * @return The composite factory
     */
    private ModuleDescriptorFactory getChainedModuleDescriptorFactory(ModuleDescriptorFactory originalFactory, PluginArtifact pluginArtifact) {
        // we really don't want two of these
        synchronized (this) {
            if (moduleDescriptorFactoryTracker == null) {
                moduleDescriptorFactoryTracker = osgi.getServiceTracker(ModuleDescriptorFactory.class.getName());
            }
        }

        // Really shouldn't be null, but could be in tests since we can't mock a service tracker :(
        if (moduleDescriptorFactoryTracker != null) {
            List<ModuleDescriptorFactory> factories = new ArrayList<ModuleDescriptorFactory>();

            factories.add(transformedDescriptorFactory);
            factories.add(originalFactory);
            Object[] serviceObjs = moduleDescriptorFactoryTracker.getServices();

            // Add all the dynamic module descriptor factories registered as osgi services
            if (serviceObjs != null) {
                for (Object fac : serviceObjs) {
                    ModuleDescriptorFactory dynFactory = (ModuleDescriptorFactory) fac;
                    if (dynFactory instanceof ListableModuleDescriptorFactory) {
                        for (Class<ModuleDescriptor<?>> descriptor : ((ListableModuleDescriptorFactory) dynFactory).getModuleDescriptorClasses()) {
                            // This will only work for classes not in inner jars and breaks on first non-match
                            if (!pluginArtifact.doesResourceExist(descriptor.getName().replace('.', '/') + ".class")) {
                                factories.add((ModuleDescriptorFactory) fac);
                                break;
                            } else {
                                log.info("Detected a module descriptor - " + descriptor.getName() + " - which is also present in " +
                                        "jar to be installed.  Skipping its module descriptor factory.");
                            }
                        }
                    } else {
                        factories.add((ModuleDescriptorFactory) fac);
                    }
                }
            }

            // Catch all unknown descriptors as unrecognised
            factories.add(new UnrecognisedModuleDescriptorFallbackFactory());

            return new ChainModuleDescriptorFactory(factories.toArray(new ModuleDescriptorFactory[factories.size()]));
        } else {
            return originalFactory;
        }


    }

    private PluginArtifact createOsgiPluginJar(PluginArtifact pluginArtifact) {
        File transformedFile = getPluginTransformer().transform(pluginArtifact, osgi.getHostComponentRegistrations());
        return new JarPluginArtifact(transformedFile);
    }

    private Plugin reportUnloadablePlugin(File file, Exception e) {
        log.error("Unable to load plugin: " + file, e);

        UnloadablePlugin plugin = new UnloadablePlugin();
        plugin.setErrorText("Unable to load plugin: " + e.getMessage());
        return plugin;
    }
}
