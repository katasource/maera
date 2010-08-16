package org.maera.plugin.manager;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.*;
import org.maera.plugin.descriptors.MockUnusedModuleDescriptor;
import org.maera.plugin.descriptors.RequiresRestart;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.factories.LegacyDynamicPluginFactory;
import org.maera.plugin.factories.XmlDynamicPluginFactory;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.impl.AbstractDelegatingPlugin;
import org.maera.plugin.impl.StaticPlugin;
import org.maera.plugin.loaders.DirectoryPluginLoader;
import org.maera.plugin.loaders.PluginLoader;
import org.maera.plugin.loaders.classloading.AbstractClassLoaderTest;
import org.maera.plugin.manager.store.MemoryPluginPersistentStateStore;
import org.maera.plugin.mock.MockAnimalModuleDescriptor;
import org.maera.plugin.repositories.FilePluginInstaller;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class DefaultPluginManagerLongRunningTest extends AbstractClassLoaderTest {

    private DirectoryPluginLoader directoryPluginLoader;
    /**
     * the object being tested
     */
    private DefaultPluginManager manager;
    private DefaultModuleDescriptorFactory moduleDescriptorFactory; // we should be able to use the interface here?
    private PluginEventManager pluginEventManager;
    private List<PluginLoader> pluginLoaders;

    private PluginPersistentStateStore pluginStateStore;

    @Before
    public void setUp() throws Exception {
        pluginEventManager = new DefaultPluginEventManager();

        pluginStateStore = new MemoryPluginPersistentStateStore();
        pluginLoaders = new ArrayList<PluginLoader>();
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());

        manager = new DefaultPluginManager(pluginStateStore, pluginLoaders, moduleDescriptorFactory, new DefaultPluginEventManager());
    }

    @After
    public void tearDown() throws Exception {
        manager = null;
        moduleDescriptorFactory = null;
        pluginLoaders = null;
        pluginStateStore = null;

        if (directoryPluginLoader != null) {
            directoryPluginLoader.shutDown();
            directoryPluginLoader = null;
        }
    }

    @Test
    public void testAddPluginsWithDependencyIssuesNoResolution() throws Exception {
        final Plugin servicePlugin = new EnableInPassPlugin("service.plugin", 4);
        final Plugin clientPlugin = new EnableInPassPlugin("client.plugin", 1);

        manager.addPlugins(null, Arrays.asList(servicePlugin, clientPlugin));

        assertTrue(clientPlugin.getPluginState() == PluginState.ENABLED);
        assertFalse(servicePlugin.getPluginState() == PluginState.ENABLED);
    }

    @Test
    public void testEnableFailed() throws PluginParseException {
        final Mock mockPluginLoader = new Mock(PluginLoader.class);
        final Plugin plugin = new StaticPlugin() {

            public PluginState enableInternal() {
                return PluginState.DISABLED;
            }

            public void disableInternal() {
                // do nothing
            }
        };
        plugin.setKey("foo");
        plugin.setEnabledByDefault(false);
        plugin.setPluginInformation(new PluginInformation());

        mockPluginLoader.expectAndReturn("loadAllPlugins", C.ANY_ARGS, Collections.singletonList(plugin));

        final PluginLoader proxy = (PluginLoader) mockPluginLoader.proxy();
        pluginLoaders.add(proxy);
        manager.init();

        assertEquals(1, manager.getPlugins().size());
        assertEquals(0, manager.getEnabledPlugins().size());
        assertFalse(plugin.getPluginState() == PluginState.ENABLED);
        manager.enablePlugin("foo");
        assertEquals(1, manager.getPlugins().size());
        assertEquals(0, manager.getEnabledPlugins().size());
        assertFalse(plugin.getPluginState() == PluginState.ENABLED);
    }

    @Test
    public void testInstallPluginTwiceWithDifferentName() throws Exception {
        createFillAndCleanTempPluginDirectory();

        FileUtils.cleanDirectory(pluginsTestDir);
        final File plugin1 = new PluginJarBuilder("plugin").addPluginInformation("some.key", "My name", "1.0", 1).addResource("foo.txt", "foo").addJava(
                "my.MyClass", "package my; public class MyClass {}").build();

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        manager.setPluginInstaller(new FilePluginInstaller(pluginsTestDir));

        final String pluginKey = manager.installPlugin(new JarPluginArtifact(plugin1));

        assertTrue(new File(pluginsTestDir, plugin1.getName()).exists());

        final Plugin installedPlugin = manager.getPlugin(pluginKey);
        assertNotNull(installedPlugin);
        InputStream s0 = installedPlugin.getClassLoader().getResourceAsStream("foo.txt");
        assertNotNull(s0);
        s0.close();
        InputStream s1 = installedPlugin.getClassLoader().getResourceAsStream("bar.txt");
        assertNull(s1);
        assertNotNull(installedPlugin.getClassLoader().loadClass("my.MyClass"));
        try {
            installedPlugin.getClassLoader().loadClass("my.MyNewClass");
            fail("Expected ClassNotFoundException for unknown class");
        }
        catch (final ClassNotFoundException e) {
            // expected
        }

        // sleep to ensure the new plugin is picked up
        Thread.sleep(1000);

        final File plugin2 = new PluginJarBuilder("plugin").addPluginInformation("some.key", "My name", "1.0", 1)
                .addResource("bar.txt", "bar").addJava("my.MyNewClass", "package my; public class MyNewClass {}")
                .build();

        // reinstall the plugin
        final String pluginKey2 = manager.installPlugin(new JarPluginArtifact(plugin2));

        assertFalse(new File(pluginsTestDir, plugin1.getName()).exists());
        assertTrue(new File(pluginsTestDir, plugin2.getName()).exists());

        final Plugin installedPlugin2 = manager.getPlugin(pluginKey2);
        assertNotNull(installedPlugin2);
        assertEquals(1, manager.getEnabledPlugins().size());
        InputStream s2 = installedPlugin2.getClassLoader().getResourceAsStream("foo.txt");
        assertNull(s2);
        InputStream s3 = installedPlugin2.getClassLoader().getResourceAsStream("bar.txt");
        assertNotNull(s3);
        s3.close();
        assertNotNull(installedPlugin2.getClassLoader().loadClass("my.MyNewClass"));
        try {
            installedPlugin2.getClassLoader().loadClass("my.MyClass");
            fail("Expected ClassNotFoundException for unknown class");
        }
        catch (final ClassNotFoundException e) {
            // expected
        }
    }

    @Test
    public void testInstallPluginTwiceWithSameName() throws Exception {
        createFillAndCleanTempPluginDirectory();

        FileUtils.cleanDirectory(pluginsTestDir);
        final File plugin = File.createTempFile("plugin", ".jar");
        plugin.delete();
        File jar = new PluginJarBuilder("plugin")
                .addPluginInformation("some.key", "My name", "1.0", 1)
                .addResource("foo.txt", "foo")
                .addJava("my.MyClass",
                        "package my; public class MyClass {}")
                .build();
        FileUtils.moveFile(jar, plugin);

        final DefaultPluginManager manager = makeClassLoadingPluginManager();
        manager.setPluginInstaller(new FilePluginInstaller(pluginsTestDir));

        final String pluginKey = manager.installPlugin(new JarPluginArtifact(plugin));

        assertTrue(new File(pluginsTestDir, plugin.getName()).exists());

        final Plugin installedPlugin = manager.getPlugin(pluginKey);
        assertNotNull(installedPlugin);
        InputStream s0 = installedPlugin.getClassLoader().getResourceAsStream("foo.txt");
        assertNotNull(s0);
        s0.close();
        assertNull(installedPlugin.getClassLoader().getResourceAsStream("bar.txt"));
        assertNotNull(installedPlugin.getClassLoader().loadClass("my.MyClass"));
        try {
            installedPlugin.getClassLoader().loadClass("my.MyNewClass");
            fail("Expected ClassNotFoundException for unknown class");
        }
        catch (final ClassNotFoundException e) {
            // expected
        }

        // sleep to ensure the new plugin is picked up
        Thread.sleep(1000);

        File jartmp = new PluginJarBuilder("plugin")
                .addPluginInformation("some.key", "My name", "1.0", 1)
                .addResource("bar.txt", "bar")
                .addJava("my.MyNewClass",
                        "package my; public class MyNewClass {}")
                .build();
        plugin.delete();
        FileUtils.moveFile(jartmp, plugin);

        // reinstall the plugin
        final String pluginKey2 = manager.installPlugin(new JarPluginArtifact(plugin));

        assertTrue(new File(pluginsTestDir, plugin.getName()).exists());

        final Plugin installedPlugin2 = manager.getPlugin(pluginKey2);
        assertNotNull(installedPlugin2);
        assertEquals(1, manager.getEnabledPlugins().size());
        assertNull(installedPlugin2.getClassLoader().getResourceAsStream("foo.txt"));
        InputStream s1 = installedPlugin2.getClassLoader().getResourceAsStream("bar.txt");
        assertNotNull(s1);
        s1.close();
        assertNotNull(installedPlugin2.getClassLoader().loadClass("my.MyNewClass"));
        try {
            installedPlugin2.getClassLoader().loadClass("my.MyClass");
            fail("Expected ClassNotFoundException for unknown class");
        }
        catch (final ClassNotFoundException e) {
            // expected
        }
    }

    private DefaultPluginManager makeClassLoadingPluginManager() throws PluginParseException {
        directoryPluginLoader = new DirectoryPluginLoader(pluginsTestDir, Arrays.asList(new LegacyDynamicPluginFactory(
                PluginAccessor.Descriptor.FILENAME), new XmlDynamicPluginFactory("foo")), pluginEventManager);
        pluginLoaders.add(directoryPluginLoader);

        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        manager.init();
        return manager;
    }

    public class NothingModuleDescriptor extends MockUnusedModuleDescriptor {

    }

    @RequiresRestart
    public static class RequiresRestartModuleDescriptor extends MockUnusedModuleDescriptor {

    }

    private static class EnableInPassPlugin extends AbstractDelegatingPlugin {

        private int pass;

        public EnableInPassPlugin(final String key, final int pass) {
            super(new StaticPlugin());
            this.pass = pass;
            setKey(key);
        }

        @Override
        public void enable() {
            if (--pass <= 0) {
                super.enable();
            }
        }
    }
}
