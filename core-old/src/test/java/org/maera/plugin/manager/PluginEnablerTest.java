package org.maera.plugin.manager;

import com.mockobjects.dynamic.Mock;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.*;
import org.maera.plugin.impl.StaticPlugin;
import org.maera.plugin.util.PluginUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class PluginEnablerTest {

    private PluginEnabler enabler;

    @Before
    public void setUp() {
        Mock mockAccessor = new Mock(PluginAccessor.class);
        Mock mockController = new Mock(PluginController.class);
        enabler = new PluginEnabler((PluginAccessor) mockAccessor.proxy(), (PluginController) mockController.proxy());
    }

    @Test
    public void testEnable() {
        Plugin plugin = new MyPlugin("foo");

        enabler.enable(Arrays.asList(plugin));
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
    }

    @Test
    public void testEnableAllRecursivelyEnablesDependencies() {
        Plugin plugin = new MyPlugin("foo", "foo2");
        Plugin plugin2 = new MyPlugin("foo2", "foo3");
        Plugin plugin3 = new MyPlugin("foo3");
        MockPluginAccessor accessor = new MockPluginAccessor();
        accessor.addPlugin(plugin);
        accessor.addPlugin(plugin2);
        accessor.addPlugin(plugin3);

        enabler = new PluginEnabler(accessor, mock(PluginController.class));

        // Enable a single plugin, which will recursively enable all deps
        enabler.enableAllRecursively(Arrays.asList(plugin));
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
        assertEquals(PluginState.ENABLED, plugin2.getPluginState());
        assertEquals(PluginState.ENABLED, plugin3.getPluginState());
    }

    @Test
    public void testEnableWithCustomTimeout() {

        Plugin plugin = new MyPlugin("foo") {

            @Override
            protected PluginState enableInternal() throws PluginException {
                return PluginState.ENABLING;
            }
        };

        try {
            System.setProperty(PluginUtils.MAERA_PLUGINS_ENABLE_WAIT, "1");
            long start = System.currentTimeMillis();
            enabler = new PluginEnabler(mock(PluginAccessor.class), mock(PluginController.class));
            enabler.enable(Arrays.asList(plugin));
            long end = System.currentTimeMillis();
            assertTrue(end - start < 5000);
            assertEquals(PluginState.ENABLING, plugin.getPluginState());
        }
        finally {
            System.clearProperty(PluginUtils.MAERA_PLUGINS_ENABLE_WAIT);
        }
    }

    @Test
    public void testRecursiveCircular() {
        Plugin plugin = new MyPlugin("foo", "foo2");
        Plugin plugin2 = new MyPlugin("foo2", "foo3");
        Plugin plugin3 = new MyPlugin("foo3", "foo");

        enabler.enable(Arrays.asList(plugin, plugin2, plugin3));
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
        assertEquals(PluginState.ENABLED, plugin2.getPluginState());
        assertEquals(PluginState.ENABLED, plugin3.getPluginState());
    }

    @Test
    public void testRecursiveEnable() {
        Plugin plugin = new MyPlugin("foo", "foo2");
        Plugin plugin2 = new MyPlugin("foo2", "foo3");
        Plugin plugin3 = new MyPlugin("foo3");

        enabler.enable(Arrays.asList(plugin, plugin2, plugin3));
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
        assertEquals(PluginState.ENABLED, plugin2.getPluginState());
        assertEquals(PluginState.ENABLED, plugin3.getPluginState());
    }

    public static class MyPlugin extends StaticPlugin {

        private final Set<String> deps;

        public MyPlugin(String key, String... deps) {
            setKey(key);
            this.deps = new HashSet<String>(Arrays.asList(deps));
            setPluginState(PluginState.DISABLED);
        }

        @Override
        public Set<String> getRequiredPlugins() {
            return deps;
        }
    }
}