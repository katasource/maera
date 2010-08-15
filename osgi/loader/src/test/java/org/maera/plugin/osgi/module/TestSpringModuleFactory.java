package org.maera.plugin.osgi.module;

import junit.framework.TestCase;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.impl.AbstractPlugin;
import org.maera.plugin.module.ContainerAccessor;
import org.maera.plugin.module.ContainerManagedPlugin;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.osgi.spring.SpringContainerAccessor;

import java.io.InputStream;
import java.net.URL;

import static org.mockito.Mockito.*;

public class TestSpringModuleFactory extends TestCase {
    ModuleFactory moduleCreator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        moduleCreator = new BeanPrefixModuleFactory();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCreateBeanFailedUsingHostContainer() throws Exception {
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        final Plugin plugin = mock(Plugin.class);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);

        try {
            moduleCreator.createModule("springBean", moduleDescriptor);
            fail("Spring not available for non osgi plugins. Bean creation should have failed");
        }
        catch (IllegalArgumentException e) {
            assertEquals("Failed to resolve 'springBean'. You cannot use 'bean' prefix with non-OSGi plugins", e.getMessage());
        }
    }

    public void testCreateBeanUsingSpring() throws Exception {
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        final SpringContainerAccessor springContextAccessor = mock(SpringContainerAccessor.class);
        final Plugin plugin = new MockContainerManagedPlugin(springContextAccessor);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        final Object springBean = new Object();
        when(springContextAccessor.getBean("springBean")).thenReturn(springBean);
        final Object obj = moduleCreator.createModule("springBean", moduleDescriptor);
        verify(springContextAccessor).getBean("springBean");
        assertEquals(obj, springBean);
    }

    private class MockContainerManagedPlugin extends AbstractPlugin implements ContainerManagedPlugin {
        private ContainerAccessor containerAccessor;

        public MockContainerManagedPlugin(ContainerAccessor containerAccessor) {
            this.containerAccessor = containerAccessor;
        }

        public ContainerAccessor getContainerAccessor() {
            return containerAccessor;
        }

        public boolean isUninstallable() {
            return false;
        }

        public boolean isDeleteable() {
            return false;
        }

        public boolean isDynamicallyLoaded() {
            return false;
        }

        public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException {
            return (Class<T>) Class.forName(clazz);
        }

        public ClassLoader getClassLoader() {
            return null;
        }

        public URL getResource(final String path) {
            return null;
        }

        public InputStream getResourceAsStream(final String name) {
            return null;
        }
    }

}
