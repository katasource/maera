package org.maera.plugin.osgi.performance;

import org.junit.Test;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.io.IOException;

/**
 * Tests the plugin framework handling restarts correctly
 */
public class LegacyFrameworkRestartTest extends AbstractFrameworkRestartTest {
    protected void addPlugin(File dir, int x) throws IOException {
        new PluginJarBuilder("restart-test")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin" + x + "'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <dummy key='dum1'/>",
                        "</maera-plugin>")
                .build(dir);
    }

    @Test
    public void testMultiplePlugins() throws Exception {
        startPluginFramework();
        pluginManager.shutdown();
    }
}