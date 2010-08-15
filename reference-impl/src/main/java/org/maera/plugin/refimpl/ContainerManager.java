package org.maera.plugin.refimpl;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.event.config.EventThreadPoolConfiguration;
import com.atlassian.event.config.ListenerHandlersConfiguration;
import com.atlassian.event.internal.*;
import com.atlassian.event.spi.EventDispatcher;
import com.atlassian.event.spi.EventExecutorFactory;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.maera.plugin.*;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.hostcontainer.HostContainer;
import org.maera.plugin.hostcontainer.SimpleConstructorHostContainer;
import org.maera.plugin.main.AtlassianPlugins;
import org.maera.plugin.main.PluginsConfiguration;
import org.maera.plugin.main.PluginsConfigurationBuilder;
import org.maera.plugin.module.ClassPrefixModuleFactory;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.module.PrefixDelegatingModuleFactory;
import org.maera.plugin.module.PrefixModuleFactory;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.osgi.module.BeanPrefixModuleFactory;
import org.maera.plugin.refimpl.servlet.SimpleContentTypeResolver;
import org.maera.plugin.refimpl.servlet.SimpleServletContextFactory;
import org.maera.plugin.refimpl.webresource.SimpleWebResourceIntegration;
import org.maera.plugin.servlet.DefaultServletModuleManager;
import org.maera.plugin.servlet.DownloadStrategy;
import org.maera.plugin.servlet.PluginResourceDownload;
import org.maera.plugin.servlet.ServletModuleManager;
import org.maera.plugin.servlet.descriptors.ServletContextListenerModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletContextParamModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletModuleDescriptor;
import org.maera.plugin.util.Assertions;
import org.maera.plugin.webresource.*;
import org.maera.plugin.webresource.transformer.WebResourceTransformerModuleDescriptor;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * A simple class that behaves like Spring's ContainerManager class.
 */
public class ContainerManager {
    /**
     * the name of the bundled plugins zip file to use
     */
    private static final String BUNDLED_PLUGINS_ZIP = "/atlassian-bundled-plugins.zip";

    private final ServletModuleManager servletModuleManager;
    private final SimpleWebResourceIntegration webResourceIntegration;
    private final WebResourceManager webResourceManager;
    private final OsgiContainerManager osgiContainerManager;
    private final PluginAccessor pluginAccessor;
    private final HostComponentProvider hostComponentProvider;
    private final DefaultModuleDescriptorFactory moduleDescriptorFactory;
    private final Map<Class<?>, Object> publicContainer;
    private final AtlassianPlugins plugins;

    private final HostContainer hostContainer;
    private static ContainerManager instance;
    private final List<DownloadStrategy> downloadStrategies;

    public ContainerManager(final ServletContext servletContext) {
        instance = this;
        webResourceIntegration = new SimpleWebResourceIntegration(servletContext);

        // Delegating host container since the real one requires the created object map, which won't be available until later
        final HostContainer delegatingHostContainer = new HostContainer() {
            public <T> T create(final Class<T> moduleClass) throws IllegalArgumentException {
                return hostContainer.create(moduleClass);
            }
        };

        moduleDescriptorFactory = new DefaultModuleDescriptorFactory(delegatingHostContainer);

        moduleDescriptorFactory.addModuleDescriptor("servlet", ServletModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-filter", ServletFilterModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-context-param", ServletContextParamModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("servlet-context-listener", ServletContextListenerModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("web-resource", WebResourceModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("web-resource-transformer", WebResourceTransformerModuleDescriptor.class);

        final DefaultPackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration(determineVersion());
        scannerConfig.setServletContext(servletContext);

        final List<String> packageIncludes = new ArrayList<String>(scannerConfig.getPackageIncludes());
        packageIncludes.add("org.bouncycastle*");
        packageIncludes.add("org.dom4j*");
        packageIncludes.add("javax.servlet*");
        packageIncludes.add("com.google*");
        packageIncludes.add("com.opensymphony.module.sitemesh*");

        scannerConfig.setPackageIncludes(packageIncludes);
        scannerConfig.setPackageVersions(new HashMap<String, String>() {{
            put("javax.servlet", "2.5");
            put("javax.servlet.http", "2.5");
            put("org.apache.commons.logging", "1.1.1");
        }});
        hostComponentProvider = new SimpleHostComponentProvider();

        File osgiCache;
        if (System.getProperty("osgi.cache") != null) {
            osgiCache = makeSureDirectoryExists(System.getProperty("osgi.cache"));
        } else {
            osgiCache = makeSureDirectoryExists(servletContext, "/WEB-INF/osgi-cache");
        }

        final PluginsConfiguration config = new PluginsConfigurationBuilder()
                .useLegacyDynamicPluginDeployer(true)
                .bundledPluginUrl(this.getClass().getResource(BUNDLED_PLUGINS_ZIP))
                .bundledPluginCacheDirectory(makeSureDirectoryExists(servletContext, "/WEB-INF/bundled-plugins"))
                .pluginDirectory(makeSureDirectoryExists(servletContext, "/WEB-INF/plugins"))
                .moduleDescriptorFactory(moduleDescriptorFactory)
                .packageScannerConfiguration(scannerConfig)
                .hostComponentProvider(hostComponentProvider)
                .osgiPersistentCache(osgiCache)
                .pluginStateStore(new DefaultPluginPersistentStateStore(osgiCache))
                .applicationKey("refapp")
                .build();

        PrefixDelegatingModuleFactory moduleFactory = new PrefixDelegatingModuleFactory(ImmutableSet.<PrefixModuleFactory>of(new BeanPrefixModuleFactory()));
        plugins = new AtlassianPlugins(config);

        final PluginEventManager pluginEventManager = plugins.getPluginEventManager();
        osgiContainerManager = plugins.getOsgiContainerManager();

        servletModuleManager = new DefaultServletModuleManager(servletContext, pluginEventManager);
        pluginAccessor = plugins.getPluginAccessor();

        final PluginResourceLocator pluginResourceLocator = new PluginResourceLocatorImpl(webResourceIntegration, new SimpleServletContextFactory(servletContext));
        final PluginResourceDownload pluginDownloadStrategy = new PluginResourceDownload(pluginResourceLocator, new SimpleContentTypeResolver(), "UTF-8");

        webResourceManager = new WebResourceManagerImpl(pluginResourceLocator, webResourceIntegration, new DefaultResourceBatchingConfiguration());

        publicContainer = new HashMap<Class<?>, Object>();
        publicContainer.put(PluginController.class, plugins.getPluginController());
        publicContainer.put(PluginAccessor.class, pluginAccessor);

        // TODO: should re-use event publisher from plugin event manager
        publicContainer.put(EventPublisher.class, createEventPublisher());

        publicContainer.put(ServletModuleManager.class, servletModuleManager);
        publicContainer.put(WebResourceManager.class, webResourceManager);
        publicContainer.put(Map.class, publicContainer);
        publicContainer.put(ModuleFactory.class, moduleFactory);

        hostContainer = new SimpleConstructorHostContainer(publicContainer);
        moduleFactory.addPrefixModuleFactory(new ClassPrefixModuleFactory(hostContainer));

        try {
            plugins.start();
        }
        catch (final PluginParseException e) {
            throw new RuntimeException(e);
        }

        downloadStrategies = new ArrayList<DownloadStrategy>();
        downloadStrategies.add(pluginDownloadStrategy);
    }

    private EventPublisher createEventPublisher() {
        EventThreadPoolConfiguration threadPoolConfiguration = new EventThreadPoolConfigurationImpl();
        EventExecutorFactory factory = new EventExecutorFactoryImpl(threadPoolConfiguration);
        EventDispatcher dispatcher = new AsynchronousAbleEventDispatcher(factory);
        ListenerHandlersConfiguration listenerHandlersConfiguration = new ListenerHandlerConfigurationImpl();
        EventPublisher publisher = new EventPublisherImpl(dispatcher, listenerHandlersConfiguration);
        return publisher;
    }

    private String determineVersion() {
        InputStream in = null;
        final Properties props = new Properties();
        try {
            in = getClass().getClassLoader().getResourceAsStream("META-INF/maven/org.maera.plugins/atlassian-plugins-core/pom.properties");
            if (in != null) {
                props.load(in);
                return props.getProperty("version");
            }
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
        finally {
            IOUtils.closeQuietly(in);
        }
        return null;
    }

    private File makeSureDirectoryExists(final ServletContext servletContext, final String relativePath) {
        return makeSureDirectoryExists(servletContext.getRealPath(relativePath));
    }

    private File makeSureDirectoryExists(final String path) {
        final File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Could not create directory <" + dir + ">");
        }
        return dir;
    }

    public static synchronized void setInstance(final ContainerManager mgr) {
        instance = mgr;
    }

    public static synchronized ContainerManager getInstance() {
        return instance;
    }

    public ServletModuleManager getServletModuleManager() {
        return servletModuleManager;
    }

    public OsgiContainerManager getOsgiContainerManager() {
        return osgiContainerManager;
    }

    public PluginAccessor getPluginAccessor() {
        return pluginAccessor;
    }

    public HostComponentProvider getHostComponentProvider() {
        return hostComponentProvider;
    }

    public ModuleDescriptorFactory getModuleDescriptorFactory() {
        return moduleDescriptorFactory;
    }

    public List<DownloadStrategy> getDownloadStrategies() {
        return downloadStrategies;
    }

    public WebResourceManager getWebResourceManager() {
        return webResourceManager;
    }

    public WebResourceIntegration getWebResourceIntegration() {
        return webResourceIntegration;
    }

    void shutdown() {
        plugins.stop();
    }

    private class SimpleHostComponentProvider implements HostComponentProvider {
        public void provide(final ComponentRegistrar componentRegistrar) {
            Assertions.notNull("publicContainer", publicContainer);
            for (final Map.Entry<Class<?>, Object> entry : publicContainer.entrySet()) {
                final String name = StringUtils.uncapitalize(entry.getKey().getSimpleName());
                componentRegistrar.register(entry.getKey()).forInstance(entry.getValue()).withName(name);
            }

        }
    }
}
