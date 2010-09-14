package org.maera.plugin.predicate;

import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.descriptors.MockUnusedModuleDescriptor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testing {@link ModuleDescriptorOfClassPredicate}
 */
public class ModuleDescriptorOfClassPredicateTest {

    private ModuleDescriptorPredicate<Object> moduleDescriptorPredicate;

    @Before
    public void setUp() throws Exception {
        moduleDescriptorPredicate = new ModuleDescriptorOfClassPredicate<Object>(ModuleDescriptorStubA.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCannotCreateWithNullModuleDescritptorClassesArray() {
        new ModuleDescriptorOfClassPredicate<Object>((Class<ModuleDescriptor<Object>>[]) null);
    }

    @Test
    public void testDoesNotMatchModuleWithModuleDescriptorClassExtendingButNotExactlyMatchingClass() {
        assertTrue(moduleDescriptorPredicate.matches(new ModuleDescriptorStubB()));
    }

    @Test
    public void testDoesNotMatchModuleWithModuleDescriptorClassNotMatchingClass() {
        assertFalse(moduleDescriptorPredicate.matches(new MockUnusedModuleDescriptor()));
    }

    @Test
    public void testMatchesModuleWithModuleDescriptorClassExactlyMatchingClass() {
        assertTrue(moduleDescriptorPredicate.matches(new ModuleDescriptorStubA()));
    }

    private static class ModuleDescriptorStubA extends AbstractModuleDescriptor<Object> {

        public Object getModule() {
            return null;
        }
    }

    private static class ModuleDescriptorStubB extends ModuleDescriptorStubA {

    }
}
