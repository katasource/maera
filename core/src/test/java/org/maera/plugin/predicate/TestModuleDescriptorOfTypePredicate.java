package org.maera.plugin.predicate;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;

/**
 * Testing {@link ModuleDescriptorOfTypePredicate}
 */
public class TestModuleDescriptorOfTypePredicate extends TestCase {
    public void testMatchesModuleWithModuleDescriptorMatchingType() {
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.matchAndReturn("getModuleDescriptorClass", C.ANY_ARGS, ModuleDescriptorStubA.class);

        final ModuleDescriptorPredicate<Object> moduleDescriptorPredicate = new ModuleDescriptorOfTypePredicate<Object>(
                (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy(), "test-module-type");
        assertTrue(moduleDescriptorPredicate.matches(new ModuleDescriptorStubB()));
    }

    public void testDoesNotMatchModuleWithModuleDescriptorNotMatchingType() {
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        mockModuleDescriptorFactory.matchAndReturn("getModuleDescriptorClass", C.ANY_ARGS, ModuleDescriptorStubB.class);

        final ModuleDescriptorPredicate<Object> moduleDescriptorPredicate = new ModuleDescriptorOfTypePredicate<Object>(
                (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy(), "test-module-type");
        assertFalse(moduleDescriptorPredicate.matches(new AbstractModuleDescriptor<Object>(ModuleFactory.LEGACY_MODULE_FACTORY) {
            @Override
            public Object getModule() {
                throw new UnsupportedOperationException();
            }
        }));
    }

    private static class ModuleDescriptorStubA extends AbstractModuleDescriptor<Object> {
        @Override
        public Object getModule() {
            return null;
        }
    }

    private static class ModuleDescriptorStubB extends ModuleDescriptorStubA {
    }
}
