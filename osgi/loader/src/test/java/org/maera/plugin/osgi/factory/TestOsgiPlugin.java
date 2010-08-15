package org.maera.plugin.osgi.factory;

import junit.framework.TestCase;
import org.maera.plugin.PluginState;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.descriptors.RequiresRestart;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginContainerRefreshedEvent;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.Hashtable;

import static org.mockito.Mockito.*;

public class TestOsgiPlugin extends TestCase {
    private Bundle bundle;
    private OsgiPlugin plugin;
    private BundleContext bundleContext;
    private Dictionary dict;
    private OsgiPluginHelper helper;

    @Override
    public void setUp() {


        bundle = mock(Bundle.class);
        dict = new Hashtable();
        dict.put(Constants.BUNDLE_DESCRIPTION, "desc");
        dict.put(Constants.BUNDLE_VERSION, "1.0");
        when(bundle.getHeaders()).thenReturn(dict);
        bundleContext = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bundleContext);

        helper = mock(OsgiPluginHelper.class);
        when(helper.getBundle()).thenReturn(bundle);

        plugin = new OsgiPlugin(mock(PluginEventManager.class), helper);
    }

    @Override
    public void tearDown() {
        bundle = null;
        plugin = null;
        bundleContext = null;
    }

    public void testEnabled() throws BundleException {
        when(bundle.getState()).thenReturn(Bundle.RESOLVED);
        plugin.enable();
        verify(bundle).start();
    }

    public void testDisabled() throws BundleException {
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        plugin.disable();
        verify(bundle).stop();
    }

    public void testDisabledOnNonDynamicPlugin() throws BundleException {
        plugin.addModuleDescriptor(new StaticModuleDescriptor());
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        plugin.disable();
        verify(bundle, never()).stop();
    }

    public void testUninstall() throws BundleException {
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        plugin.uninstall();
        assertEquals(plugin.getPluginState(), PluginState.UNINSTALLED);
    }

    public void testOnSpringRefresh() {
        plugin.setKey("plugin-key");
        when(bundle.getState()).thenReturn(Bundle.RESOLVED);
        plugin.enable();
        PluginContainerRefreshedEvent event = new PluginContainerRefreshedEvent(new Object(), "plugin-key");
        plugin.onSpringContextRefresh(event);
        assertEquals(PluginState.ENABLED, plugin.getPluginState());
    }

    public void testQuickOnSpringRefresh() throws BundleException, InterruptedException {
        plugin.setKey("plugin-key");
        when(bundle.getState()).thenReturn(Bundle.RESOLVED);

        final ConcurrentStateEngine states = new ConcurrentStateEngine("bundle-starting", "spring-created", "bundle-started", "mid-start", "end");
        when(bundle.getBundleContext()).thenAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                states.tryNextState("bundle-started", "mid-start");
                return mock(BundleContext.class);
            }
        });

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                states.state("bundle-starting");
                Thread t = new Thread() {
                    public void run() {
                        PluginContainerRefreshedEvent event = new PluginContainerRefreshedEvent(new Object(), "plugin-key");
                        states.tryNextState("bundle-starting", "spring-created");
                        plugin.onSpringContextRefresh(event);
                    }
                };
                t.start();
                states.tryNextState("spring-created", "bundle-started");
                return null;
            }
        }).when(bundle).start();

        plugin.enable();


        states.tryNextState("mid-start", "end");

        assertEquals(PluginState.ENABLED, plugin.getPluginState());
    }

    public void testOnSpringRefreshNotEnabling() {
        plugin.setKey("plugin-key");
        PluginContainerRefreshedEvent event = new PluginContainerRefreshedEvent(new Object(), "plugin-key");
        when(bundle.getState()).thenReturn(Bundle.ACTIVE);
        plugin.disable();
        plugin.onSpringContextRefresh(event);
        assertEquals(PluginState.DISABLED, plugin.getPluginState());
    }

    @RequiresRestart
    public static class StaticModuleDescriptor<Object> extends AbstractModuleDescriptor {
        public Object getModule() {
            return null;
        }
    }


}