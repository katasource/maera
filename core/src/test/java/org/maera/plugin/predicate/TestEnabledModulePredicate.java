package org.maera.plugin.predicate;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.PluginAccessor;

/**
 * Testing {@link EnabledModulePredicate}
 */
public class TestEnabledModulePredicate extends TestCase {
    private static final String MODULE_TEST_KEY = "some-module-key";

    private Mock mockPluginAccessor;
    private ModuleDescriptorPredicate moduleDescriptorPredicate;
    private ModuleDescriptor moduleDescriptor;

    protected void setUp() throws Exception {
        mockPluginAccessor = new Mock(PluginAccessor.class);
        moduleDescriptorPredicate = new EnabledModulePredicate((PluginAccessor) mockPluginAccessor.proxy());

        final Mock mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        mockModuleDescriptor.matchAndReturn("getCompleteKey", MODULE_TEST_KEY);
        moduleDescriptor = (ModuleDescriptor) mockModuleDescriptor.proxy();
    }

    protected void tearDown() throws Exception {
        moduleDescriptorPredicate = null;
        mockPluginAccessor = null;
        moduleDescriptor = null;
    }

    public void testCannotCreateWithNullPluginAccessor() {
        try {
            new EnabledModulePredicate(null);
            fail("Constructor should have thrown illegal argument exception.");
        }
        catch (IllegalArgumentException e) {
            // noop
        }
    }

    public void testMatchesEnabledModule() {
        mockPluginAccessor.matchAndReturn("isPluginModuleEnabled", C.eq(MODULE_TEST_KEY), true);
        assertTrue(moduleDescriptorPredicate.matches(moduleDescriptor));
    }

    public void testDoesNotMatchDisabledModule() {
        mockPluginAccessor.matchAndReturn("isPluginModuleEnabled", C.eq(MODULE_TEST_KEY), false);
        assertFalse(moduleDescriptorPredicate.matches(moduleDescriptor));
    }
}
