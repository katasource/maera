package org.maera.plugin.osgi.performance;

import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.osgi.DummyModuleDescriptor;
import org.maera.plugin.osgi.PluginInContainerTestBase;
import org.maera.plugin.osgi.SomeInterface;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.osgi.framework.Bundle;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tests the plugin framework handling restarts correctly
 */
public abstract class FrameworkRestartTestBase extends PluginInContainerTestBase {
    private static final int NUM_HOST_COMPONENTS = 200;
    private static final int NUM_PLUGINS = 50;
    HostComponentProvider prov = null;
    DefaultModuleDescriptorFactory factory = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        prov = new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
                for (int x = 0; x < NUM_HOST_COMPONENTS; x++) {
                    registrar.register(SomeInterface.class).forInstance(new SomeInterface() {
                    });
                }
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(8);
        for (int x = 0; x < NUM_PLUGINS; x++) {
            final int run = x;
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        addPlugin(pluginsDir, run);
                    }
                    catch (Exception e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
            });

        }
        executor.shutdown();
        executor.awaitTermination(300, TimeUnit.SECONDS);
        // warm up the cache
        startPluginFramework();
        pluginManager.shutdown();
    }

    protected abstract void addPlugin(File dir, int x) throws Exception;

    protected void startPluginFramework() throws Exception {
        initPluginManager(prov, factory);
        assertEquals(pluginManager.getPlugins().size(), pluginManager.getEnabledPlugins().size());
        for (Bundle bundle : osgiContainerManager.getBundles()) {
            assertEquals("Bundle " + bundle.getSymbolicName() + " was not active: " + bundle.getState(), Bundle.ACTIVE, bundle.getState());
        }
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMultiplePlugins() throws Exception {
        startPluginFramework();
        pluginManager.shutdown();
    }
}
