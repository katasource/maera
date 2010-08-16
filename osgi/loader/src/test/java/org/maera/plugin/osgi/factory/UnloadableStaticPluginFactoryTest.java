package org.maera.plugin.osgi.factory;

import org.junit.Test;
import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.impl.UnloadablePlugin;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnloadableStaticPluginFactoryTest {

    @Test
    public void testCanCreate() {
        UnloadableStaticPluginFactory factory = new UnloadableStaticPluginFactory("foo.xml");
        PluginArtifact artifact = mock(PluginArtifact.class);
        when(artifact.getResourceAsStream("foo.xml")).thenReturn(new ByteArrayInputStream(
                "<maera-plugin key=\"foo\" />".getBytes()
        ));
        assertEquals("foo", factory.canCreate(artifact));
    }

    @Test
    public void testCanCreateWithNoDescriptor() {
        UnloadableStaticPluginFactory factory = new UnloadableStaticPluginFactory("foo.xml");
        PluginArtifact artifact = mock(PluginArtifact.class);
        when(artifact.getResourceAsStream("foo.xml")).thenReturn(null);
        assertEquals(null, factory.canCreate(artifact));
    }

    @Test
    public void testCanCreateWithOsgi() {
        UnloadableStaticPluginFactory factory = new UnloadableStaticPluginFactory("foo.xml");
        PluginArtifact artifact = mock(PluginArtifact.class);
        when(artifact.getResourceAsStream("foo.xml")).thenReturn(new ByteArrayInputStream(
                "<maera-plugin key=\"foo\" plugins-version=\"2\"/>".getBytes()
        ));
        assertEquals(null, factory.canCreate(artifact));
    }

    @Test
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

    @Test
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
