package org.maera.plugin.loaders.classloading;

import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginArtifactFactory;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.factories.PluginFactory;
import org.maera.plugin.impl.UnloadablePlugin;
import org.maera.plugin.loaders.ScanningPluginLoader;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class ScanningPluginLoaderTest {

    private PluginEventManager pluginEventManager;

    @Before
    public void setUp() throws Exception {
        pluginEventManager = new DefaultPluginEventManager();
    }

    @Test
    public void testFactoryThrowsError() {
        testFactoryThrowsThrowable(new NoClassDefFoundError());
    }

    @Test
    public void testFactoryThrowsRuntimeException() {
        testFactoryThrowsThrowable(new IllegalArgumentException());
    }

    @Test
    public void testOnShutdown() {
        PluginArtifactFactory artFactory = mock(PluginArtifactFactory.class);
        PluginArtifact art = mock(PluginArtifact.class);
        when(artFactory.create((URI) anyObject())).thenReturn(art);

        DeploymentUnit unit = new DeploymentUnit(new File("foo.jar"));
        Scanner scanner = mock(Scanner.class);
        when(scanner.getDeploymentUnits()).thenReturn(Collections.singletonList(unit));
        PluginFactory factory = mock(PluginFactory.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isUninstallable()).thenReturn(true);

        when(factory.canCreate(art)).thenReturn("foo");
        when(factory.create(art, null)).thenReturn(plugin);

        ScanningPluginLoader loader = new ScanningPluginLoader(scanner, Arrays.asList(factory), artFactory, pluginEventManager);
        loader.loadAllPlugins(null);
        loader.onShutdown(null);
        verify(plugin).uninstall();
    }

    @Test
    public void testOnShutdownButUninstallable() {
        PluginArtifactFactory artFactory = mock(PluginArtifactFactory.class);
        PluginArtifact art = mock(PluginArtifact.class);
        when(artFactory.create((URI) anyObject())).thenReturn(art);

        DeploymentUnit unit = new DeploymentUnit(new File("foo.jar"));
        Scanner scanner = mock(Scanner.class);
        when(scanner.getDeploymentUnits()).thenReturn(Collections.singletonList(unit));
        PluginFactory factory = mock(PluginFactory.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isUninstallable()).thenReturn(false);

        when(factory.canCreate(art)).thenReturn("foo");
        when(factory.create(art, null)).thenReturn(plugin);

        ScanningPluginLoader loader = new ScanningPluginLoader(scanner, Arrays.asList(factory), artFactory, pluginEventManager);
        loader.loadAllPlugins(null);
        loader.onShutdown(null);
        verify(plugin, never()).uninstall();
    }

    @Test
    public void testPostProcessCalledAlways() {
        final AtomicBoolean called = new AtomicBoolean(false);
        PluginArtifactFactory artFactory = mock(PluginArtifactFactory.class);
        PluginArtifact art = mock(PluginArtifact.class);
        when(artFactory.create((URI) anyObject())).thenReturn(art);

        DeploymentUnit unit = new DeploymentUnit(new File("foo.jar"));
        Scanner scanner = mock(Scanner.class);
        when(scanner.getDeploymentUnits()).thenReturn(Collections.singletonList(unit));
        PluginFactory factory = mock(PluginFactory.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isUninstallable()).thenReturn(true);

        when(factory.canCreate(art)).thenReturn("foo");
        when(factory.create(art, null)).thenReturn(plugin);
        ScanningPluginLoader loader = new ScanningPluginLoader(scanner, Arrays.asList(factory), artFactory, pluginEventManager) {

            @Override
            protected Plugin postProcess(Plugin plugin) {
                called.set(true);
                return plugin;
            }
        };
        loader.loadAllPlugins(null);
        assertTrue(called.get());

        called.set(false);
        DeploymentUnit unit2 = new DeploymentUnit(new File("bar.jar"));
        when(scanner.scan()).thenReturn(Collections.singletonList(unit2));
        PluginArtifact art2 = mock(PluginArtifact.class);
        when(artFactory.create((URI) anyObject())).thenReturn(art2);
        when(factory.canCreate(art2)).thenReturn("bar");
        when(factory.create(art2, null)).thenReturn(plugin);
        loader.addFoundPlugins(null);
        assertTrue(called.get());
    }

    private void testFactoryThrowsThrowable(Throwable e) {
        PluginArtifactFactory artFactory = mock(PluginArtifactFactory.class);
        PluginArtifact art = mock(PluginArtifact.class);
        when(artFactory.create((URI) anyObject())).thenReturn(art);


        DeploymentUnit unit = new DeploymentUnit(new File("foo.jar"));
        Scanner scanner = mock(Scanner.class);
        when(scanner.getDeploymentUnits()).thenReturn(Collections.singletonList(unit));
        PluginFactory factory = mock(PluginFactory.class);

        when(factory.canCreate(art)).thenReturn("foo");
        when(factory.create(art, null)).thenThrow(e);

        ScanningPluginLoader loader = new ScanningPluginLoader(scanner, Arrays.asList(factory), artFactory, pluginEventManager);
        Collection<Plugin> plugins = loader.loadAllPlugins(null);
        assertNotNull(plugins);
        assertEquals(1, plugins.size());
        assertTrue(plugins.iterator().next() instanceof UnloadablePlugin);
    }
}
