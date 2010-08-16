package org.maera.plugin.predicate;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.PluginAccessor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testing {@link EnabledModulePredicate}
 */
public class EnabledModulePredicateTest {

    private static final String MODULE_TEST_KEY = "some-module-key";

    private Mock mockPluginAccessor;
    private ModuleDescriptor moduleDescriptor;
    private ModuleDescriptorPredicate moduleDescriptorPredicate;

    @Before
    public void setUp() throws Exception {
        mockPluginAccessor = new Mock(PluginAccessor.class);
        moduleDescriptorPredicate = new EnabledModulePredicate((PluginAccessor) mockPluginAccessor.proxy());

        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.matchAndReturn("getCompleteKey", MODULE_TEST_KEY);
        moduleDescriptor = (ModuleDescriptor) mockModuleDescriptor.proxy();
    }

    @After
    public void tearDown() throws Exception {
        moduleDescriptorPredicate = null;
        mockPluginAccessor = null;
        moduleDescriptor = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotCreateWithNullPluginAccessor() {
        new EnabledModulePredicate(null);
    }

    @Test
    public void testDoesNotMatchDisabledModule() {
        mockPluginAccessor.matchAndReturn("isPluginModuleEnabled", C.eq(MODULE_TEST_KEY), false);
        assertFalse(moduleDescriptorPredicate.matches(moduleDescriptor));
    }

    @Test
    public void testMatchesEnabledModule() {
        mockPluginAccessor.matchAndReturn("isPluginModuleEnabled", C.eq(MODULE_TEST_KEY), true);
        assertTrue(moduleDescriptorPredicate.matches(moduleDescriptor));
    }
}
