package org.maera.plugin.loaders;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.maera.plugin.*;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.factories.LegacyDynamicPluginFactory;
import org.maera.plugin.factories.PluginFactory;
import org.maera.plugin.factories.XmlDynamicPluginFactory;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.impl.UnloadablePlugin;
import org.maera.plugin.loaders.classloading.AbstractClassLoaderTest;
import org.maera.plugin.mock.MockAnimalModuleDescriptor;
import org.maera.plugin.mock.MockBear;
import org.maera.plugin.mock.MockMineralModuleDescriptor;
import org.maera.plugin.test.PluginJarBuilder;
import org.maera.plugin.util.collect.CollectionUtil;
import org.maera.plugin.util.collect.Predicate;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.*;
import static org.maera.plugin.util.collect.CollectionUtil.filter;

public class DirectoryPluginLoaderTest extends AbstractClassLoaderTest {

    public static final String BAD_PLUGIN_JAR = "bad-plugins/crap-plugin.jar";

    private static final List<PluginFactory> DEFAULT_PLUGIN_FACTORIES = unmodifiableList(asList(
            new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME), new XmlDynamicPluginFactory("foo")));

    private DirectoryPluginLoader loader;
    private DefaultModuleDescriptorFactory moduleDescriptorFactory;
    private PluginEventManager pluginEventManager;

    @Before
    public void setUp() throws Exception {
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        pluginEventManager = new DefaultPluginEventManager();
        createFillAndCleanTempPluginDirectory();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(pluginsTestDir);
    }

    @Test
    public void testFoundPlugin() throws PluginParseException, IOException {
        //delete paddington for the timebeing
        final File paddington = new File(pluginsTestDir + File.separator + PADDINGTON_JAR);
        paddington.delete();

        addTestModuleDecriptors();
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        loader.loadAllPlugins(moduleDescriptorFactory);

        //restore paddington to test plugins dir
        FileUtils.copyDirectory(pluginsDirectory, pluginsTestDir);

        Collection<Plugin> col = loader.addFoundPlugins(moduleDescriptorFactory);
        assertEquals(1, col.size());
        // next time we shouldn't find any new plugins
        col = loader.addFoundPlugins(moduleDescriptorFactory);
        assertEquals(0, col.size());
    }

    @Test
    public void testInstallPluginTwice() throws URISyntaxException, IOException, PluginParseException, InterruptedException {
        FileUtils.cleanDirectory(pluginsTestDir);
        final File plugin = new File(pluginsTestDir, "some-plugin.jar");
        new PluginJarBuilder("plugin").addPluginInformation("some.key", "My name", "1.0", 1).addResource("foo.txt", "foo").build().renameTo(plugin);

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertNotNull((plugins.iterator().next()).getResource("foo.txt"));
        assertNull((plugins.iterator().next()).getResource("bar.txt"));

        Thread.currentThread();
        // sleep to ensure the new plugin is picked up
        Thread.sleep(1000);

        plugin.delete(); //delete the old plugin artifact to make windows happy
        new PluginJarBuilder("plugin").addPluginInformation("some.key", "My name", "1.0", 1).addResource("bar.txt", "bar").build().renameTo(plugin);
        plugins = loader.addFoundPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertNull((plugins.iterator().next()).getResource("foo.txt"));
        assertNotNull((plugins.iterator().next()).getResource("bar.txt"));
        assertTrue(plugin.exists());

    }

    @Test
    public void testInvalidPluginHandled() throws IOException, PluginParseException {
        createJarFile("evilplugin.jar", PluginAccessor.Descriptor.FILENAME, pluginsTestDir.getAbsolutePath());

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals("evil jar wasn't loaded, but other plugins were", pluginsTestDir.list(new FilenameFilter() {

            public boolean accept(final File directory, final String fileName) {
                return fileName.endsWith(".jar");
            }
        }).length, plugins.size());

        assertEquals(1, CollectionUtil.toList(filter(plugins, new Predicate<Plugin>() {
            public boolean evaluate(final Plugin input) {
                return input instanceof UnloadablePlugin;
            }
        })).size());
    }

    @Ignore
    @Test
    public void testMaeraPlugin() throws Exception {
        addTestModuleDecriptors();
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        assertEquals(2, plugins.size());

        for (final Plugin plugin : plugins) {
            assertTrue(plugin.getName().equals("Test Class Loaded Plugin") || plugin.getName().equals("Test Class Loaded Plugin 2"));

            if (plugin.getName().equals("Test Class Loaded Plugin")) // asserts for first plugin
            {
                assertEquals("Test Class Loaded Plugin", plugin.getName());
                assertEquals("test.maera.plugin.classloaded", plugin.getKey());
                assertEquals(1, plugin.getModuleDescriptors().size());
                final MockAnimalModuleDescriptor paddingtonDescriptor = (MockAnimalModuleDescriptor) plugin.getModuleDescriptor("paddington");
                paddingtonDescriptor.enabled();
                assertEquals("Paddington Bear", paddingtonDescriptor.getName());
                final MockBear paddington = (MockBear) paddingtonDescriptor.getModule();
                assertEquals("org.maera.plugin.mock.MockPaddington", paddington.getClass().getName());
            } else if (plugin.getName().equals("Test Class Loaded Plugin 2")) // asserts for second plugin
            {
                assertEquals("Test Class Loaded Plugin 2", plugin.getName());
                assertEquals("test.maera.plugin.classloaded2", plugin.getKey());
                assertEquals(1, plugin.getModuleDescriptors().size());
                final MockAnimalModuleDescriptor poohDescriptor = (MockAnimalModuleDescriptor) plugin.getModuleDescriptor("pooh");
                poohDescriptor.enabled();
                assertEquals("Pooh Bear", poohDescriptor.getName());
                final MockBear pooh = (MockBear) poohDescriptor.getModule();
                assertEquals("org.maera.plugin.mock.MockPooh", pooh.getClass().getName());
            } else {
                fail("What plugin name?!");
            }
        }
    }

    @Test
    public void testMixedFactories() throws URISyntaxException, IOException, PluginParseException, InterruptedException {
        FileUtils.cleanDirectory(pluginsTestDir);
        final File plugin = new File(pluginsTestDir, "some-plugin.jar");
        new PluginJarBuilder("plugin").addPluginInformation("some.key", "My name", "1.0", 1).addResource("foo.txt", "foo").build().renameTo(plugin);
        FileUtils.writeStringToFile(new File(pluginsTestDir, "foo.xml"), "<maera-plugin key=\"jim\"></maera-plugin>");

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(2, plugins.size());
    }

    @Test
    public void testNoFoundPlugins() throws PluginParseException {
        addTestModuleDecriptors();
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        Collection<Plugin> col = loader.addFoundPlugins(moduleDescriptorFactory);
        assertFalse(col.isEmpty());

        col = loader.addFoundPlugins(moduleDescriptorFactory);
        assertTrue(col.isEmpty());
    }

    @Test
    public void testPluginWithBadDescriptor() throws Exception, IOException, PluginParseException, InterruptedException {
        FileUtils.cleanDirectory(pluginsTestDir);
        File pluginJar = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-pluasdfasdf")
                .build(pluginsTestDir);

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertTrue(plugins.iterator().next() instanceof UnloadablePlugin);
        assertEquals(pluginJar.getName(), plugins.iterator().next().getKey());
    }

    @Test
    public void testPluginWithModuleDescriptorWithNoKey() throws Exception, IOException, PluginParseException, InterruptedException {
        FileUtils.cleanDirectory(pluginsTestDir);
        new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object/>",
                        "</maera-plugin>")
                .build(pluginsTestDir);

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertTrue(plugins.iterator().next() instanceof UnloadablePlugin);
        assertEquals("test.plugin", plugins.iterator().next().getKey());
    }

    @Test
    public void testRemovePlugin() throws PluginException, IOException {
        addTestModuleDecriptors();
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        //duplicate the paddington plugin before removing the original
        //the duplicate will be used to restore the deleted original after the test

        final Iterator<Plugin> iter = plugins.iterator();

        Plugin paddingtonPlugin = null;

        while (iter.hasNext()) {
            final Plugin plugin = iter.next();

            if (plugin.getName().equals("Test Class Loaded Plugin")) {
                paddingtonPlugin = plugin;
                break;
            }
        }

        if (paddingtonPlugin == null) {
            fail("Can't find test plugin 1 (paddington)");
        }

        loader.removePlugin(paddingtonPlugin);
    }

    @Test
    public void testSupportsAdditionAndRemoval() {
        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);
        assertTrue(loader.supportsAddition());
        assertTrue(loader.supportsRemoval());
    }

    @Test
    public void testUnknownPluginArtifact() throws URISyntaxException, IOException, PluginParseException, InterruptedException {
        FileUtils.cleanDirectory(pluginsTestDir);
        FileUtils.writeStringToFile(new File(pluginsTestDir, "foo.bob"), "<an>");

        loader = new DirectoryPluginLoader(pluginsTestDir, DEFAULT_PLUGIN_FACTORIES, pluginEventManager);

        final Collection<Plugin> plugins = loader.loadAllPlugins(moduleDescriptorFactory);
        assertEquals(1, plugins.size());
        assertTrue(plugins.iterator().next() instanceof UnloadablePlugin);
    }

    private void addTestModuleDecriptors() {
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
    }

    private void createJarFile(final String jarname, final String jarEntry, final String saveDir) throws IOException {
        final OutputStream os = new FileOutputStream(saveDir + File.separator + jarname);
        final JarOutputStream plugin1 = new JarOutputStream(os);
        final JarEntry jarEntry1 = new JarEntry(jarEntry);

        plugin1.putNextEntry(jarEntry1);
        plugin1.closeEntry();
        plugin1.flush();
        plugin1.close();
    }
}
