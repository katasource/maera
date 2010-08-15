package org.maera.plugin.osgi;

import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;

public class TestShutdown extends PluginInContainerTestBase {
    public void testShutdown() throws Exception {
        File pluginJar = new PluginJarBuilder("shutdowntest")
                .addPluginInformation("shutdown", "foo", "1.0")
                .build();
        initPluginManager(null);
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.shutdown();
    }
}

