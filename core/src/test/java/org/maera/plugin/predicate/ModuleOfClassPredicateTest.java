package org.maera.plugin.predicate;

import com.mockobjects.dynamic.Mock;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testing {@link ModuleOfClassPredicate}
 */
public class ModuleOfClassPredicateTest {

    private final static Class TEST_MODULE_CLASS = Object.class;

    private Mock mockModuleDescriptor;
    private ModuleDescriptor<Object> moduleDescriptor;
    private ModuleDescriptorPredicate<Object> moduleDescriptorPredicate;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        moduleDescriptorPredicate = new ModuleOfClassPredicate(TEST_MODULE_CLASS);

        mockModuleDescriptor = new Mock(ModuleDescriptor.class);
        moduleDescriptor = (ModuleDescriptor) mockModuleDescriptor.proxy();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotCreateWithNullClass() {
        new ModuleOfClassPredicate<Object>(null);
    }

    @Test
    public void testDoesNotMatchModuleNotExtendingClass() {
        mockModuleDescriptor.matchAndReturn("getModuleClass", int.class); //Primitives don't extend Object
        assertFalse(moduleDescriptorPredicate.matches(moduleDescriptor));
    }

    @Test
    public void testMatchesModuleExtendingClass() {
        mockModuleDescriptor.matchAndReturn("getModuleClass", this.getClass());
        assertTrue(moduleDescriptorPredicate.matches(moduleDescriptor));
    }
}
