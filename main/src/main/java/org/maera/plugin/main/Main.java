package org.maera.plugin.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;

import static org.maera.plugin.main.PackageScannerConfigurationBuilder.packageScannerConfiguration;
import static org.maera.plugin.main.PluginsConfigurationBuilder.pluginsConfiguration;

/**
 * Simple standalone class for starting the plugin framework.  Creates a directory called "plugins" in the current
 * directory and scans it every 5 seconds for new plugins.
 * <p/>
 * For embedded use, use the {@link MaeraPlugins} facade directly
 */
public class Main {

    private static final transient Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        final File pluginDir = new File("plugins");
        pluginDir.mkdir();
        System.out.println("Created plugins directory " + pluginDir.getAbsolutePath());

        final PluginsConfiguration config = pluginsConfiguration().pluginDirectory(pluginDir).packageScannerConfiguration(
                packageScannerConfiguration().packagesToInclude("org.apache.*", "org.maera.*", "org.dom4j*").packagesVersions(
                        Collections.singletonMap("org.apache.log4j", "1.2.15")).build()).build();
        final MaeraPlugins plugins = new MaeraPlugins(config);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Cleaning up...");
                plugins.stop();
            }
        });

        plugins.start();

        final Thread hotDeploy = new Thread("Hot Deploy") {
            @Override
            public void run() {
                while (true) {

                    plugins.getPluginController().scanForNewPlugins();
                    try {
                        Thread.sleep(5000);
                    }
                    catch (final InterruptedException e) {
                        // ignore
                        break;
                    }
                }
            }
        };
        hotDeploy.start();
    }

}
