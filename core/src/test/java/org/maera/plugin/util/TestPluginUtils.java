package org.maera.plugin.util;

import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.Plugin;
import org.maera.plugin.descriptors.MockUnusedModuleDescriptor;
import org.maera.plugin.descriptors.RequiresRestart;

import java.util.Arrays;

public class TestPluginUtils extends TestCase {
    public void testDoesPluginRequireRestartDevMode() {
        try {
            System.setProperty("maera.dev.mode", "true");
            Mock mockPlugin = new Mock(Plugin.class);
            assertFalse(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
            mockPlugin.verify();
        }
        finally {
            System.clearProperty("maera.dev.mode");
        }

        Mock mockPlugin2 = new Mock(Plugin.class);
        mockPlugin2.expectAndReturn("getModuleDescriptors", Arrays.asList(new DynamicModuleDescriptor(), new RequiresRestartModuleDescriptor()));
        assertTrue(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin2.proxy()));
        mockPlugin2.verify();
    }

    public void testDoesPluginRequireRestart() {
        Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getModuleDescriptors", Arrays.asList(new DynamicModuleDescriptor(), new RequiresRestartModuleDescriptor()));
        assertTrue(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
        mockPlugin.verify();

        mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getModuleDescriptors", Arrays.asList(new DynamicModuleDescriptor()));
        assertFalse(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
        mockPlugin.verify();

        mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getModuleDescriptors", Arrays.asList());
        assertFalse(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
        mockPlugin.verify();

        mockPlugin = new Mock(Plugin.class);
        mockPlugin.expectAndReturn("getModuleDescriptors", Arrays.asList(new RequiresRestartModuleDescriptor()));
        assertTrue(PluginUtils.doesPluginRequireRestart((Plugin) mockPlugin.proxy()));
        mockPlugin.verify();
    }

    static class DynamicModuleDescriptor extends MockUnusedModuleDescriptor {
    }

    @RequiresRestart
    static class RequiresRestartModuleDescriptor extends MockUnusedModuleDescriptor {
    }
}
