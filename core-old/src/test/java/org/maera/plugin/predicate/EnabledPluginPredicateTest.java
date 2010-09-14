package org.maera.plugin.predicate;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testing {@link EnabledPluginPredicate}
 */
public class EnabledPluginPredicateTest {

    private static final String TEST_PLUGIN_KEY = "some-test-plugin";

    private Mock mockPluginAccessor;
    private Plugin plugin;
    private PluginPredicate pluginPredicate;

    @Before
    public void setUp() throws Exception {
        mockPluginAccessor = new Mock(PluginAccessor.class);

        pluginPredicate = new EnabledPluginPredicate((PluginAccessor) mockPluginAccessor.proxy());

        final Mock mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getKey", TEST_PLUGIN_KEY);
        plugin = (Plugin) mockPlugin.proxy();
    }

    @After
    public void tearDown() throws Exception {
        pluginPredicate = null;
        mockPluginAccessor = null;
        plugin = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotCreateWithNullPluginAccessor() {
        new EnabledPluginPredicate(null);
    }

    @Test
    public void testDoesNotMatchDisabledPlugin() {
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.eq(TEST_PLUGIN_KEY), false);
        assertFalse(pluginPredicate.matches(plugin));
    }

    @Test
    public void testMatchesEnabledPlugin() {
        mockPluginAccessor.matchAndReturn("isPluginEnabled", C.eq(TEST_PLUGIN_KEY), true);
        assertTrue(pluginPredicate.matches(plugin));
    }
}
