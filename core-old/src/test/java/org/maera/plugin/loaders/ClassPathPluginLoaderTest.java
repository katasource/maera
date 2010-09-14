package org.maera.plugin.loaders;

import org.junit.Test;
import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.Plugin;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.mock.MockAnimalModuleDescriptor;
import org.maera.plugin.mock.MockMineralModuleDescriptor;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class ClassPathPluginLoaderTest {

    @Test
    public void testAtlassianPlugin() throws Exception {
        ClassPathPluginLoader loader = new ClassPathPluginLoader("test-maera-plugin.xml");
        DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("vegetable", MockMineralModuleDescriptor.class);

        Collection plugins = loader.loadAllPlugins(moduleDescriptorFactory);

        Plugin plugin = (Plugin) plugins.iterator().next();
        assertEquals("Test Plugin", plugin.getName());
        assertEquals("test.maera.plugin", plugin.getKey());
        assertEquals("This plugin descriptor is just used for test purposes!", plugin.getPluginInformation().getDescription());
        assertEquals(4, plugin.getModuleDescriptors().size());
        assertEquals("Bear Animal", plugin.getModuleDescriptor("bear").getName());
    }
}
