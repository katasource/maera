package org.maera.plugin.osgi.factory;

import junit.framework.TestCase;
import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.impl.UnloadablePlugin;

import java.io.ByteArrayInputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUnloadableStaticPluginFactory extends TestCase {
    public void testCanCreate() {
        UnloadableStaticPluginFactory factory = new UnloadableStaticPluginFactory("foo.xml");
        PluginArtifact artifact = mock(PluginArtifact.class);
        when(artifact.getResourceAsStream("foo.xml")).thenReturn(new ByteArrayInputStream(
                "<maera-plugin key=\"foo\" />".getBytes()
        ));
        assertEquals("foo", factory.canCreate(artifact));
    }

    public void testCanCreateWithOsgi() {
        UnloadableStaticPluginFactory factory = new UnloadableStaticPluginFactory("foo.xml");
        PluginArtifact artifact = mock(PluginArtifact.class);
        when(artifact.getResourceAsStream("foo.xml")).thenReturn(new ByteArrayInputStream(
                "<maera-plugin key=\"foo\" plugins-version=\"2\"/>".getBytes()
        ));
        assertEquals(null, factory.canCreate(artifact));
    }

    public void testCanCreateWithNoDescriptor() {
        UnloadableStaticPluginFactory factory = new UnloadableStaticPluginFactory("foo.xml");
        PluginArtifact artifact = mock(PluginArtifact.class);
        when(artifact.getResourceAsStream("foo.xml")).thenReturn(null);
        assertEquals(null, factory.canCreate(artifact));
    }

    public void testCreate() {
        UnloadableStaticPluginFactory factory = new UnloadableStaticPluginFactory("foo.xml");
        PluginArtifact artifact = mock(PluginArtifact.class);
        when(artifact.getResourceAsStream("foo.xml")).thenReturn(new ByteArrayInputStream(
                "<maera-plugin key=\"foo\" />".getBytes()
        ));
        when(artifact.toString()).thenReturn("plugin.jar");
        UnloadablePlugin plugin = (UnloadablePlugin) factory.create(artifact, new DefaultModuleDescriptorFactory(new DefaultHostContainer()));
        assertNotNull(plugin);
        assertEquals("foo", plugin.getKey());
        assertTrue(plugin.getErrorText().contains("plugin.jar"));

    }

    public void testCreateWithNoKey() {
        UnloadableStaticPluginFactory factory = new UnloadableStaticPluginFactory("foo.xml");
        PluginArtifact artifact = mock(PluginArtifact.class);
        when(artifact.getResourceAsStream("foo.xml")).thenReturn(new ByteArrayInputStream(
                "<maera-plugin />".getBytes()
        ));
        when(artifact.toString()).thenReturn("plugin.jar");
        UnloadablePlugin plugin = (UnloadablePlugin) factory.create(artifact, new DefaultModuleDescriptorFactory(new DefaultHostContainer()));
        assertNotNull(plugin);
        assertEquals(null, plugin.getKey());
        assertTrue(plugin.getErrorText().contains("plugin.jar"));

    }
}
