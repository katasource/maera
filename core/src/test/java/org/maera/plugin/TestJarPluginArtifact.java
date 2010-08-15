package org.maera.plugin;

import junit.framework.TestCase;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.io.IOException;

public class TestJarPluginArtifact extends TestCase {
    public void testGetResourceAsStream() throws IOException {
        File plugin = new PluginJarBuilder()
                .addResource("foo", "bar")
                .build();
        JarPluginArtifact artifact = new JarPluginArtifact(plugin);

        assertNotNull(artifact.getResourceAsStream("foo"));
        assertNull(artifact.getResourceAsStream("bar"));
    }
}
