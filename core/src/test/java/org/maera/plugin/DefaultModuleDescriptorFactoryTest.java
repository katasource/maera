package org.maera.plugin;

import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.mock.MockAnimalModuleDescriptor;
import org.maera.plugin.mock.MockMineralModuleDescriptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class DefaultModuleDescriptorFactoryTest {

    private DefaultModuleDescriptorFactory moduleDescriptorFactory;

    @Before
    public void setUp() {
        moduleDescriptorFactory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
    }

    @Test
    public void testInvalidModuleDescriptorType() {
        try {
            moduleDescriptorFactory.getModuleDescriptor("foobar");
            fail("Should have thrown exception");
        }
        catch (final IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (final PluginParseException e) {
            return;
        }
        catch (final ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (final InstantiationException e) {
            e.printStackTrace();
        }

        fail("Threw the wrong exception");
    }

    @Test
    public void testModuleDescriptorFactory() throws PluginParseException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        assertTrue(moduleDescriptorFactory.getModuleDescriptor("animal") instanceof MockAnimalModuleDescriptor);
        assertTrue(moduleDescriptorFactory.getModuleDescriptor("mineral") instanceof MockMineralModuleDescriptor);

        assertTrue(moduleDescriptorFactory.hasModuleDescriptor("animal"));
        assertTrue(moduleDescriptorFactory.hasModuleDescriptor("mineral"));
        assertFalse(moduleDescriptorFactory.hasModuleDescriptor("something"));

        // Test removing a module descriptor
        moduleDescriptorFactory.removeModuleDescriptorForType("mineral");

        // Ensure the removed module descriptor is not there
        assertFalse(moduleDescriptorFactory.hasModuleDescriptor("mineral"));

        // Ensure the other one is still there
        assertTrue(moduleDescriptorFactory.hasModuleDescriptor("animal"));
        assertTrue(moduleDescriptorFactory.getModuleDescriptor("animal") instanceof MockAnimalModuleDescriptor);
    }

    @Test
    public void testModuleDescriptorFactoryOnlyPermittedDescriptors() throws IllegalAccessException, PluginParseException, ClassNotFoundException, InstantiationException {
        // Add the "supported" module descriptors
        moduleDescriptorFactory.addModuleDescriptor("animal", MockAnimalModuleDescriptor.class);
        moduleDescriptorFactory.addModuleDescriptor("mineral", MockMineralModuleDescriptor.class);

        // Exclude "mineral"
        final List<String> permittedList = new ArrayList<String>();
        permittedList.add("animal");
        moduleDescriptorFactory.setPermittedModuleKeys(permittedList);
        // Try and grab the "animal" descriptor
        assertNotNull(moduleDescriptorFactory.getModuleDescriptor("animal"));

        // "mineral" is excluded, so it should return null
        assertNull(moduleDescriptorFactory.getModuleDescriptor("mineral"));
    }
}
