package org.maera.plugin.osgi;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.factories.LegacyDynamicPluginFactory;
import org.maera.plugin.factories.PluginFactory;
import org.maera.plugin.hostcontainer.SimpleConstructorHostContainer;
import org.maera.plugin.loaders.BundledPluginLoader;
import org.maera.plugin.loaders.DirectoryPluginLoader;
import org.maera.plugin.loaders.PluginLoader;
import org.maera.plugin.manager.DefaultPluginManager;
import org.maera.plugin.manager.store.MemoryPluginPersistentStateStore;
import org.maera.plugin.module.ClassPrefixModuleFactory;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.module.PrefixDelegatingModuleFactory;
import org.maera.plugin.module.PrefixModuleFactory;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.container.OsgiPersistentCache;
import org.maera.plugin.osgi.container.PackageScannerConfiguration;
import org.maera.plugin.osgi.container.felix.FelixOsgiContainerManager;
import org.maera.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import org.maera.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import org.maera.plugin.osgi.factory.OsgiBundleFactory;
import org.maera.plugin.osgi.factory.OsgiPluginFactory;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.osgi.hostcomponents.InstanceBuilder;
import org.maera.plugin.osgi.module.BeanPrefixModuleFactory;
import org.maera.plugin.repositories.FilePluginInstaller;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Base for in-container unit tests
 */
public abstract class AbstractPluginInContainerTest {

    protected File cacheDir;
    protected SimpleConstructorHostContainer hostContainer;
    protected ModuleDescriptorFactory moduleDescriptorFactory;
    protected ModuleFactory moduleFactory;
    protected OsgiContainerManager osgiContainerManager;
    protected PluginEventManager pluginEventManager;
    protected DefaultPluginManager pluginManager;
    protected File pluginsDir;
    protected File tmpDir;

    @Before
    public void setUp() throws Exception {
        tmpDir = new File("target/plugin-temp").getAbsoluteFile();
        if (tmpDir.exists()) {
            FileUtils.cleanDirectory(tmpDir);
        }
        tmpDir.mkdirs();
        cacheDir = new File(tmpDir, "cache");
        cacheDir.mkdir();
        pluginsDir = new File(tmpDir, "plugins");
        pluginsDir.mkdir();
        this.pluginEventManager = new DefaultPluginEventManager();
        moduleFactory = new PrefixDelegatingModuleFactory(ImmutableSet.<PrefixModuleFactory>of(
                new ClassPrefixModuleFactory(hostContainer),
                new BeanPrefixModuleFactory()));
        hostContainer = createHostContainer(new HashMap<Class<?>, Object>());
    }

    @After
    public void tearDown() throws Exception {
        if (osgiContainerManager != null) {
            osgiContainerManager.stop();
        }
        FileUtils.deleteDirectory(tmpDir);
        osgiContainerManager = null;
        tmpDir = null;
        pluginsDir = null;
        moduleDescriptorFactory = null;
        pluginManager = null;
        pluginEventManager = null;
        moduleFactory = null;
        hostContainer = null;
    }

    protected SimpleConstructorHostContainer createHostContainer(Map<Class<?>, Object> originalContext) {
        Map<Class<?>, Object> context = new HashMap<Class<?>, Object>(originalContext);
        context.put(ModuleFactory.class, moduleFactory);
        return new SimpleConstructorHostContainer(context);
    }

    protected void initBundlingPluginManager(final ModuleDescriptorFactory moduleDescriptorFactory, File... bundledPluginJars) throws Exception {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        final PackageScannerConfiguration scannerConfig = buildScannerConfiguration("1.0");
        HostComponentProvider requiredWrappingProvider = getWrappingHostComponentProvider(null);
        OsgiPersistentCache cache = new DefaultOsgiPersistentCache(cacheDir);
        osgiContainerManager = new FelixOsgiContainerManager(cache, scannerConfig, requiredWrappingProvider, pluginEventManager);

        final OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginAccessor.Descriptor.FILENAME, (String) null, cache, osgiContainerManager, pluginEventManager);

        final DirectoryPluginLoader loader = new DirectoryPluginLoader(pluginsDir, Arrays.<PluginFactory>asList(osgiPluginDeployer),
                new DefaultPluginEventManager());

        File zip = new File(bundledPluginJars[0].getParentFile(), "bundled-plugins.zip");
        for (File bundledPluginJar : bundledPluginJars) {
            ZipOutputStream stream = null;
            InputStream in = null;
            try {
                stream = new ZipOutputStream(new FileOutputStream(zip));
                in = new FileInputStream(bundledPluginJar);
                stream.putNextEntry(new ZipEntry(bundledPluginJar.getName()));
                IOUtils.copy(in, stream);
                stream.closeEntry();
            }
            catch (IOException ex) {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(stream);
            }
        }
        File bundledDir = new File(bundledPluginJars[0].getParentFile(), "bundled-plugins");
        final BundledPluginLoader bundledLoader = new BundledPluginLoader(zip.toURL(), bundledDir, Arrays.<PluginFactory>asList(osgiPluginDeployer),
                new DefaultPluginEventManager());

        pluginManager = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Arrays.<PluginLoader>asList(bundledLoader, loader), moduleDescriptorFactory,
                pluginEventManager);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
        pluginManager.init();
    }

    protected void initPluginManager() throws Exception {
        initPluginManager(null, new DefaultModuleDescriptorFactory(hostContainer));
    }

    protected void initPluginManager(final HostComponentProvider hostComponentProvider) throws Exception {
        initPluginManager(hostComponentProvider, new DefaultModuleDescriptorFactory(hostContainer));
    }

    protected void initPluginManager(final HostComponentProvider hostComponentProvider, final ModuleDescriptorFactory moduleDescriptorFactory, final String version)
            throws Exception {
        final PackageScannerConfiguration scannerConfig = buildScannerConfiguration(version);
        HostComponentProvider requiredWrappingProvider = getWrappingHostComponentProvider(hostComponentProvider);
        OsgiPersistentCache cache = new DefaultOsgiPersistentCache(cacheDir);
        osgiContainerManager = new FelixOsgiContainerManager(cache, scannerConfig, requiredWrappingProvider, pluginEventManager);

        final LegacyDynamicPluginFactory legacyFactory = new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME, tmpDir);
        final OsgiPluginFactory osgiPluginDeployer = new OsgiPluginFactory(PluginAccessor.Descriptor.FILENAME, (String) null, cache, osgiContainerManager, pluginEventManager);
        final OsgiBundleFactory osgiBundleFactory = new OsgiBundleFactory(osgiContainerManager, pluginEventManager);

        final DirectoryPluginLoader loader = new DirectoryPluginLoader(pluginsDir, Arrays.asList(legacyFactory, osgiPluginDeployer, osgiBundleFactory),
                new DefaultPluginEventManager());
        initPluginManager(moduleDescriptorFactory, loader);
    }

    protected void initPluginManager(final HostComponentProvider hostComponentProvider, final ModuleDescriptorFactory moduleDescriptorFactory)
            throws Exception {
        initPluginManager(hostComponentProvider, moduleDescriptorFactory, (String) null);
    }

    protected void initPluginManager(final ModuleDescriptorFactory moduleDescriptorFactory, PluginLoader loader)
            throws Exception {
        this.moduleDescriptorFactory = moduleDescriptorFactory;
        pluginManager = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), Arrays.<PluginLoader>asList(loader), moduleDescriptorFactory,
                pluginEventManager);
        pluginManager.setPluginInstaller(new FilePluginInstaller(pluginsDir));
        pluginManager.init();
    }

    private PackageScannerConfiguration buildScannerConfiguration(String version) {
        final PackageScannerConfiguration scannerConfig = new DefaultPackageScannerConfiguration(version);
        scannerConfig.getPackageIncludes().add("org.maera.plugin*");
        scannerConfig.getPackageIncludes().add("javax.servlet*");
        scannerConfig.getPackageIncludes().add("com_cenqua_clover");
        scannerConfig.getPackageExcludes().add("org.maera.plugin.osgi.bridge*");
        scannerConfig.getPackageVersions().put("org.apache.commons.logging", "1.1.1");
        return scannerConfig;
    }

    private HostComponentProvider getWrappingHostComponentProvider(final HostComponentProvider hostComponentProvider) {
        return new HostComponentProvider() {

            public void provide(final ComponentRegistrar registrar) {

                if (hostComponentProvider != null) {
                    hostComponentProvider.provide(new ComponentRegistrar() {

                        public InstanceBuilder register(Class<?>... mainInterfaces) {
                            if (!Arrays.asList(mainInterfaces).contains(PluginEventManager.class)) {
                                return registrar.register(mainInterfaces);
                            }
                            return null;
                        }
                    });
                }
                registrar.register(PluginEventManager.class).forInstance(pluginEventManager);
            }
        };
    }
}
