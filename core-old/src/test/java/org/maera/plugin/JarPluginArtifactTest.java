package org.maera.plugin;

import org.junit.Test;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class JarPluginArtifactTest {

    @Test
    public void testGetResourceAsStream() throws IOException {
        File plugin = new PluginJarBuilder()
                .addResource("foo", "bar")
                .build();
        JarPluginArtifact artifact = new JarPluginArtifact(plugin);

        assertNotNull(artifact.getResourceAsStream("foo"));
        assertNull(artifact.getResourceAsStream("bar"));
    }
}
