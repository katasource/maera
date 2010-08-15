package org.maera.plugin.main;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;

import static org.maera.plugin.main.PackageScannerConfigurationBuilder.packageScannerConfiguration;
import static org.maera.plugin.main.PluginsConfigurationBuilder.pluginsConfiguration;

/**
 * Simple standalone class for starting the plugin framework.  Creates a directory called "plugins" in the current
 * directory and scans it every 5 seconds for new plugins.
 * <p/>
 * For embedded use, use the {@link AtlassianPlugins} facade directly
 */
public class Main {

    public static void main(final String[] args) {
        initialiseLogger();
        final File pluginDir = new File("plugins");
        pluginDir.mkdir();
        System.out.println("Created plugins directory " + pluginDir.getAbsolutePath());

        final PluginsConfiguration config = pluginsConfiguration().pluginDirectory(pluginDir).packageScannerConfiguration(
                packageScannerConfiguration().packagesToInclude("org.apache.*", "com.atlassian.*", "org.dom4j*").packagesVersions(
                        Collections.singletonMap("org.apache.log4j", "1.2.15")).build()).build();
        final AtlassianPlugins plugins = new AtlassianPlugins(config);

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

    private static void initialiseLogger() {
        final Properties logProperties = new Properties();

        InputStream in = null;
        try {
            in = Main.class.getResourceAsStream("/log4j-standalone.properties");
            logProperties.load(in);
            PropertyConfigurator.configure(logProperties);
            Logger.getLogger(Main.class).info("Logging initialized.");
        }
        catch (final IOException e) {
            throw new RuntimeException("Unable to load logging");
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

}
