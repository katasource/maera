package org.maera.plugin.module;

import org.junit.Test;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.slf4j.Logger;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings({"ThrowableInstanceNeverThrown", "unchecked"})
public class PrefixDelegatingModuleFactoryTest {

    PrefixDelegatingModuleFactory prefixDelegatingModuleFactory;

    @Test
    public void testCreateBean() throws Exception {
        PrefixModuleFactory moduleFactory = mock(PrefixModuleFactory.class);
        when(moduleFactory.getPrefix()).thenReturn("jira");
        Object bean = new Object();

        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleFactory.createModule("doSomething", moduleDescriptor)).thenReturn(bean);
        this.prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(Collections.singleton(moduleFactory));

        final Object returnedBean = prefixDelegatingModuleFactory.createModule("jira:doSomething", moduleDescriptor);
        assertEquals(bean, returnedBean);
    }

    @Test
    public void testCreateBeanFailed() throws Exception {
        PrefixModuleFactory moduleFactory = mock(PrefixModuleFactory.class);
        when(moduleFactory.getPrefix()).thenReturn("bob");
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);

        this.prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(Collections.singleton(moduleFactory));

        try {
            this.prefixDelegatingModuleFactory.createModule("jira:doSomething", moduleDescriptor);

            fail("Should not return, there is no module prefix provider for jira");
        }
        catch (PluginParseException ex) {
            //Ex
            assertEquals("Failed to create a module. Prefix 'jira' not supported", ex.getMessage());
        }
        verify(moduleFactory, never()).createModule("doSomething", moduleDescriptor);
    }

    @Test
    public void testCreateBeanThrowsLinkageError() throws Exception {
        testCreateWithThrowableCausingErrorLogMessage(new LinkageError());
    }

    @Test
    public void testCreateBeanThrowsNoClassDefFoundError() throws Exception {
        testCreateWithThrowableCausingErrorLogMessage(new NoClassDefFoundError());
    }

    @Test
    public void testCreateBeanThrowsUnsatisfiedDependencyException() throws Exception {
        testCreateWithThrowableCausingErrorLogMessage(new UnsatisfiedDependencyException());
    }

    @Test
    public void testCreateBeanWithDynamicModuleFactory() throws Exception {
        PrefixModuleFactory moduleFactory = mock(PrefixModuleFactory.class);
        when(moduleFactory.getPrefix()).thenReturn("jira");

        Object bean = new Object();
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        ContainerAccessor containerAccessor = mock(ContainerAccessor.class);
        ContainerManagedPlugin plugin = mock(ContainerManagedPlugin.class);
        when(plugin.getContainerAccessor()).thenReturn(containerAccessor);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        when(containerAccessor.getBeansOfType(PrefixModuleFactory.class)).thenReturn(Collections.singleton(moduleFactory));

        when(moduleFactory.createModule("doSomething", moduleDescriptor)).thenReturn(bean);

        this.prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(Collections.<PrefixModuleFactory>emptySet());

        final Object returnedBean = this.prefixDelegatingModuleFactory.createModule("jira:doSomething", moduleDescriptor);
        assertEquals(bean, returnedBean);
    }

    private void testCreateWithThrowableCausingErrorLogMessage(Throwable throwable) {
        PrefixModuleFactory moduleFactory = mock(PrefixModuleFactory.class);
        when(moduleFactory.getPrefix()).thenReturn("jira");
        Logger log = mock(Logger.class);

        Plugin plugin = mock(Plugin.class);
        ModuleDescriptor moduleDescriptor = mock(ModuleDescriptor.class);
        when(moduleDescriptor.getPlugin()).thenReturn(plugin);
        when(moduleFactory.createModule("doSomething", moduleDescriptor)).thenThrow(throwable);

        this.prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(Collections.singleton(moduleFactory));
        this.prefixDelegatingModuleFactory.log = log;

        try {
            prefixDelegatingModuleFactory.createModule("jira:doSomething", moduleDescriptor);

            fail("Should not return");
        }
        catch (Throwable err) {
            verify(log).error(anyString());
        }
    }

    private static class UnsatisfiedDependencyException extends RuntimeException {

    }
}
