package org.maera.plugin.manager;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginDisabledEvent;
import org.maera.plugin.event.events.PluginEnabledEvent;
import org.maera.plugin.event.events.PluginModuleDisabledEvent;
import org.maera.plugin.event.events.PluginModuleEnabledEvent;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.event.listeners.RecordingListener;
import org.maera.plugin.factories.LegacyDynamicPluginFactory;
import org.maera.plugin.factories.XmlDynamicPluginFactory;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.loaders.DirectoryPluginLoader;
import org.maera.plugin.loaders.PluginLoader;
import org.maera.plugin.loaders.SinglePluginLoader;
import org.maera.plugin.loaders.classloading.DirectoryPluginLoaderUtils;
import org.maera.plugin.manager.store.MemoryPluginPersistentStateStore;
import org.maera.plugin.mock.MockAnimalModuleDescriptor;
import org.maera.plugin.mock.MockMineralModuleDescriptor;
import org.maera.plugin.mock.MockVegetableModuleDescriptor;
import org.maera.plugin.repositories.FilePluginInstaller;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DefaultPluginManagerEventsTest {

    private RecordingListener listener;
    private DefaultPluginManager manager;

    @Before
    public void setUp() throws Exception {
        listener = new RecordingListener(
                PluginEnabledEvent.class,
                PluginDisabledEvent.class,
                PluginModuleEnabledEvent.class,
                PluginModuleDisabledEvent.class);

        manager = buildPluginManager(listener);
        manager.init();
        listener.reset();
    }

    @Test
    public void testDisableModule() throws Exception {
        manager.disablePluginModule("test.maera.plugin:bear");

        assertListEquals(listener.getEventClasses(), PluginModuleDisabledEvent.class);
        assertListEquals(listener.getEventPluginOrModuleKeys(), "test.maera.plugin:bear");
    }

    @Test
    public void testDisableModuleWithCannotDisableDoesNotFireEvent() throws Exception {
        manager.disablePluginModule("test.maera.plugin:veg");
        assertEquals(listener.getEventClasses().size(), 0);
    }

    @Test
    public void testDisablePlugin() throws Exception {
        manager.disablePlugin("test.maera.plugin");

        assertListEquals(listener.getEventClasses(),
                PluginModuleDisabledEvent.class,
                PluginModuleDisabledEvent.class,
                PluginModuleDisabledEvent.class,
                PluginDisabledEvent.class);
        assertListEquals(listener.getEventPluginOrModuleKeys(),
                "test.maera.plugin:veg",  // a  module that can't be individually disabled can still be disabled with the plugin
                "test.maera.plugin:gold", // modules in reverse order to enable
                "test.maera.plugin:bear",
                "test.maera.plugin");
    }

    @Test
    public void testEnableDisabledByDefaultPlugin() throws Exception {
        manager.enablePlugin("test.disabled.plugin");

        assertListEquals(listener.getEventClasses(), PluginEnabledEvent.class);
        assertListEquals(listener.getEventPluginOrModuleKeys(), "test.disabled.plugin");

        listener.reset();
        manager.enablePluginModule("test.disabled.plugin:gold");

        assertListEquals(listener.getEventClasses(), PluginModuleEnabledEvent.class);
        assertListEquals(listener.getEventPluginOrModuleKeys(), "test.disabled.plugin:gold");
    }

    @Test
    public void testEnableModule() throws Exception {
        manager.disablePluginModule("test.maera.plugin:bear");
        listener.reset();
        manager.enablePluginModule("test.maera.plugin:bear");

        assertListEquals(listener.getEventClasses(), PluginModuleEnabledEvent.class);
        assertListEquals(listener.getEventPluginOrModuleKeys(), "test.maera.plugin:bear");
    }

    @Test
    public void testEnablePlugin() throws Exception {
        manager.disablePlugin("test.maera.plugin");
        listener.reset();
        manager.enablePlugin("test.maera.plugin");

        assertListEquals(listener.getEventClasses(),
                PluginModuleEnabledEvent.class,
                PluginModuleEnabledEvent.class,
                PluginModuleEnabledEvent.class,
                PluginEnabledEvent.class);
        assertListEquals(listener.getEventPluginOrModuleKeys(),
                "test.maera.plugin:bear",
                "test.maera.plugin:gold",
                "test.maera.plugin:veg",
                "test.maera.plugin");
    }

    @Test
    @Ignore
    public void testInitialisationEvents() throws Exception {
        DefaultPluginManager manager = buildPluginManager(listener);
        manager.init();

        assertListEquals(listener.getEventClasses(),
                PluginModuleEnabledEvent.class,
                PluginModuleEnabledEvent.class,
                PluginModuleEnabledEvent.class,
                PluginEnabledEvent.class,
                PluginModuleEnabledEvent.class,
                PluginEnabledEvent.class,
                PluginModuleEnabledEvent.class,
                PluginEnabledEvent.class);
        assertListEquals(listener.getEventPluginOrModuleKeys(),
                "test.maera.plugin:bear",
                "test.maera.plugin:gold",
                "test.maera.plugin:veg",
                "test.maera.plugin",
                "test.maera.plugin.classloaded:paddington",
                "test.maera.plugin.classloaded",
                "test.maera.plugin.classloaded2:pooh",
                "test.maera.plugin.classloaded2");
    }

    @Test
    @Ignore
    public void testInstallPlugin() throws Exception {
        // have to uninstall one of the directory plugins
        manager.uninstall(manager.getPlugin("test.maera.plugin.classloaded2"));
        listener.reset();
        File pluginJar = new File(DirectoryPluginLoaderUtils.getTestPluginsDirectory(),
                "pooh-test-plugin.jar");
        manager.installPlugin(new JarPluginArtifact(pluginJar));

        assertListEquals(listener.getEventClasses(),
                PluginModuleEnabledEvent.class,
                PluginEnabledEvent.class);
    }

    @Test
    @Ignore
    public void testUninstallPlugin() throws Exception {
        // have to uninstall one of the directory plugins
        manager.uninstall(manager.getPlugin("test.maera.plugin.classloaded2"));

        assertListEquals(listener.getEventClasses(),
                PluginModuleDisabledEvent.class,
                PluginDisabledEvent.class);
    }

    private List<PluginLoader> buildPluginLoaders(PluginEventManager pluginEventManager, File pluginTempDirectory) {
        List<PluginLoader> pluginLoaders = new ArrayList<PluginLoader>();
        pluginLoaders.add(new SinglePluginLoader("test-maera-plugin.xml"));
        pluginLoaders.add(new SinglePluginLoader("test-disabled-plugin.xml"));
        DirectoryPluginLoader directoryPluginLoader = new DirectoryPluginLoader(
                pluginTempDirectory,
                Arrays.asList(new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME),
                        new XmlDynamicPluginFactory("foo")),
                pluginEventManager);
        pluginLoaders.add(directoryPluginLoader);
        return pluginLoaders;
    }

    private DefaultPluginManager buildPluginManager(RecordingListener listener) throws Exception {
        PluginEventManager pluginEventManager = new DefaultPluginEventManager();
        pluginEventManager.register(listener);

        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("vegetable", MockVegetableModuleDescriptor.class);

        File pluginTempDirectory = DirectoryPluginLoaderUtils.copyTestPluginsToTempDirectory();
        List<PluginLoader> pluginLoaders = buildPluginLoaders(pluginEventManager, pluginTempDirectory);

        DefaultPluginManager manager = new DefaultPluginManager(new MemoryPluginPersistentStateStore(), pluginLoaders,
                moduleDescriptorFactory, pluginEventManager);
        manager.setPluginInstaller(new FilePluginInstaller(pluginTempDirectory));

        return manager;
    }

    // yeah, the expected values should come first in jUnit, but varargs are so convenient...
    private static void assertListEquals(List actual, Object... expected) {
        String message = "Expected list was: " + Arrays.toString(expected) + ", " +
                "but actual was: " + actual;
        assertEquals(message, expected.length, actual.size());
        for (int i = 0; i < actual.size(); i++) {
            assertEquals(message, expected[i], actual.get(i));
        }
    }
}
