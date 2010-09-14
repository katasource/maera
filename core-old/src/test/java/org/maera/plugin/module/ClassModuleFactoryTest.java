package org.maera.plugin.module;

import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.hostcontainer.HostContainer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassModuleFactoryTest {

    private HostContainer hostContainer;
    ModuleFactory moduleCreator;

    @Before
    public void setUp() throws Exception {
        hostContainer = mock(HostContainer.class);
        moduleCreator = new ClassPrefixModuleFactory(hostContainer);
    }

    @Test
    public void testCreateBeanUsingHostContainer() throws Exception {
        @SuppressWarnings("unchecked")
        final ModuleDescriptor<Object> moduleDescriptor = mock(ModuleDescriptor.class);
        final Plugin plugin = mock(Plugin.class);
        when(plugin.<Object>loadClass(eq("myBean"), (Class) anyObject())).thenReturn(Object.class);

        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        when(moduleDescriptor.getModuleClass()).thenReturn(Object.class);
        final Object object = new Object();
        when(hostContainer.create(Object.class)).thenReturn(object);

        final Object bean = moduleCreator.createModule("myBean", moduleDescriptor);
        assertEquals(object, bean);
    }

    @Test
    public void testCreateBeanUsingPluginContainer() throws Exception {
        @SuppressWarnings("unchecked")
        final ModuleDescriptor<Object> moduleDescriptor = mock(ModuleDescriptor.class);
        final ContainerAccessor containerAccessor = mock(ContainerAccessor.class);
        final Plugin plugin = new MockContainerManagedPlugin(containerAccessor);

        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        final Object beanObject = new Object();
        when(containerAccessor.createBean(Object.class)).thenReturn(beanObject);
        final Object bean = moduleCreator.createModule("java.lang.Object", moduleDescriptor);
        assertEquals(beanObject, bean);
    }
}
