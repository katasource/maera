package org.maera.plugin.osgi.bridge;

import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.osgi.event.PluginServiceDependencyWaitStartingEvent;
import org.mockito.ArgumentMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;
import org.springframework.osgi.context.ConfigurableOsgiBundleApplicationContext;
import org.springframework.osgi.extender.event.BootstrappingDependencyEvent;
import org.springframework.osgi.service.importer.OsgiServiceDependency;
import org.springframework.osgi.service.importer.event.OsgiServiceDependencyWaitStartingEvent;
import org.springframework.osgi.service.importer.support.AbstractOsgiServiceImportFactoryBean;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class SpringContextEventBridgeTest {

    private SpringContextEventBridge bridge;

    private Bundle bundle;
    private PluginEventManager eventManager;

    @Before
    public void setUp() throws Exception {
        eventManager = mock(PluginEventManager.class);

        bridge = new SpringContextEventBridge(eventManager);

        Dictionary<String, String> headers = new Hashtable<String, String>();
        headers.put("Maera-Plugin-Key", "foo");

        bundle = mock(Bundle.class);
        when(bundle.getHeaders()).thenReturn(headers);
    }

    @Test
    public void testWaitingEventWithApplicationContext() throws Exception {
        ConfigurableOsgiBundleApplicationContext source = mock(ConfigurableOsgiBundleApplicationContext.class);
        when(source.getBundle()).thenReturn(bundle);
        OsgiServiceDependencyWaitStartingEvent startingEvent = new OsgiServiceDependencyWaitStartingEvent(source, mock(OsgiServiceDependency.class), 1000);
        BootstrappingDependencyEvent bootstrapEvent = new BootstrappingDependencyEvent(mock(ApplicationContext.class), bundle, startingEvent);
        bridge.onOsgiApplicationEvent(bootstrapEvent);

        verify(eventManager).broadcast(isPluginKey("foo"));
    }

    @Test
    public void testWaitingEventWithServiceFactoryBean() throws Exception {
        AbstractOsgiServiceImportFactoryBean source = mock(AbstractOsgiServiceImportFactoryBean.class);
        when(source.getBeanName()).thenReturn("bar");
        BundleContext ctx = mock(BundleContext.class);
        when(ctx.getBundle()).thenReturn(bundle);
        when(source.getBundleContext()).thenReturn(ctx);
        OsgiServiceDependencyWaitStartingEvent startingEvent = new OsgiServiceDependencyWaitStartingEvent(source, mock(OsgiServiceDependency.class), 1000);
        BootstrappingDependencyEvent bootstrapEvent = new BootstrappingDependencyEvent(mock(ApplicationContext.class), bundle, startingEvent);
        bridge.onOsgiApplicationEvent(bootstrapEvent);

        verify(eventManager).broadcast(isPluginKey("foo"));
    }

    private Object isPluginKey(String key) {
        return argThat(new PluginKeyMatcher(key));
    }

    private static class PluginKeyMatcher extends ArgumentMatcher<Object> {

        private String key;

        public PluginKeyMatcher(String key) {
            this.key = key;
        }

        public boolean matches(Object o) {
            return key.equals(((PluginServiceDependencyWaitStartingEvent) o).getPluginKey());
        }
    }
}
