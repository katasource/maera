package org.maera.plugin.predicate;

import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.ModuleDescriptor;

/**
 * Testing {@link ModuleOfClassPredicate}
 */
public class TestModuleOfClassPredicate extends TestCase {
    private final static Class TEST_MODULE_CLASS = TestCase.class;

    private ModuleDescriptorPredicate moduleDescriptorPredicate;

    private Mock mockModuleDescriptor;
    private ModuleDescriptor moduleDescriptor;

    protected void setUp() throws Exception {
        moduleDescriptorPredicate = new ModuleOfClassPredicate(TEST_MODULE_CLASS);

        mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        moduleDescriptor = (ModuleDescriptor) mockModuleDescriptor.proxy();
    }

    protected void tearDown() throws Exception {
        moduleDescriptorPredicate = null;
        moduleDescriptor = null;
        mockModuleDescriptor = null;
    }

    public void testCannotCreateWithNullClass() {
        try {
            new ModuleOfClassPredicate(null);
            fail("Constructor should have thrown illegal argument exception.");
        }
        catch (IllegalArgumentException e) {
            // noop
        }
    }

    public void testMatchesModuleExtendingClass() {
        mockModuleDescriptor.matchAndReturn("getModuleClass", this.getClass());
        assertTrue(moduleDescriptorPredicate.matches(moduleDescriptor));
    }

    public void testDoesNotMatchModuleNotExtendingClass() {
        mockModuleDescriptor.matchAndReturn("getModuleClass", Object.class);
        assertFalse(moduleDescriptorPredicate.matches(moduleDescriptor));
    }
}
