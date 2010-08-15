package org.maera.plugin.predicate;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;

/**
 * Testing {@link EnabledPluginPredicate}
 */
public class TestEnabledPluginPredicate extends TestCase {
    private static final String TEST_PLUGIN_KEY = "some-test-plugin";

    /**
     * the object to test
     */
    private PluginPredicate pluginPredicate;

    private Mock mockPluginAccessor;
    private Plugin plugin;

    protected void setUp() throws Exception {
        mockPluginAccessor = new Mock(PluginAccessor.class);

        pluginPredicate = new EnabledPluginPredicate((PluginAccessor) mockPluginAccessor.proxy());

        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getKey", TEST_PLUGIN_KEY);
        plugin = (Plugin) mockPlugin.proxy();
    }

    protected void tearDown() throws Exception {
        pluginPredicate = null;
        mockPluginAccessor = null;
        plugin = null;
    }

    public void testCannotCreateWithNullPluginAccessor() {
        try {
            new EnabledPluginPredicate(null);
            fail("Constructor should have thrown illegal argument exception.");
        }
        catch (IllegalArgumentException e) {
            // noop
        }
    }

    public void testMatchesEnabledPlugin() {
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.eq(TEST_PLUGIN_KEY), true);
        assertTrue(pluginPredicate.matches(plugin));
    }

    public void testDoesNotMatchDisabledPlugin() {
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.eq(TEST_PLUGIN_KEY), false);
        assertFalse(pluginPredicate.matches(plugin));
    }
}
