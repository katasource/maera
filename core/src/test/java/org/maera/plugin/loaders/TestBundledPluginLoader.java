package org.maera.plugin.loaders;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.factories.PluginFactory;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

public class TestBundledPluginLoader extends TestCase {
    private File pluginDir;

    public void setUp() throws IOException, URISyntaxException {
        pluginDir = new File("target/test-plugin-dir");
        pluginDir.mkdir();
    }

    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(pluginDir);
        pluginDir = null;
    }

    public void testCreateWithUnzip() throws IOException {
        File bundledZip = new PluginJarBuilder("bundledPlugins")
                .addResource("foo.txt", "foo")
                .build();

        new BundledPluginLoader(bundledZip.toURL(), pluginDir, Collections.<PluginFactory>emptyList(), new DefaultPluginEventManager());
        assertEquals(2, pluginDir.list().length);
        assertTrue(new File(pluginDir, "foo.txt").exists());
    }
}