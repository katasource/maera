package org.maera.plugin.main;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.maera.plugin.main.PluginsConfigurationBuilder.pluginsConfiguration;

public class MaeraPluginsTest extends TestCase {
    private File pluginDir;
    private File bundledPluginDir;
    private File bundledPluginZip;
    private MaeraPlugins plugins;

    @Override
    public void setUp() throws IOException {
        final File targetDir = new File("target");
        pluginDir = new File(targetDir, "plugins");
        pluginDir.mkdirs();
        FileUtils.cleanDirectory(pluginDir);
        bundledPluginDir = new File(targetDir, "bundled-plugins");
        bundledPluginDir.mkdirs();
        bundledPluginZip = new File(targetDir, "maera-bundled-plugins.zip");
    }

    @Override
    public void tearDown() throws IOException {
        FileUtils.cleanDirectory(pluginDir);
        FileUtils.cleanDirectory(bundledPluginDir);
        bundledPluginZip.delete();
        if (plugins != null) {
            plugins.stop();
        }
    }

    public void testStart() throws Exception {
        //tests
        new PluginJarBuilder().addPluginInformation("mykey", "mykey", "1.0").build(pluginDir);
        final PluginsConfiguration config = pluginsConfiguration()
                .pluginDirectory(pluginDir)
                .packageScannerConfiguration(
                        new PackageScannerConfigurationBuilder()
                                .packagesToInclude("org.apache.*", "org.maera.*", "org.dom4j*")
                                .packagesVersions(Collections.singletonMap("org.apache.commons.logging", "1.1.1"))
                                .build())
                .build();
        plugins = new MaeraPlugins(config);
        plugins.start();
        assertEquals(1, plugins.getPluginAccessor().getPlugins().size());
    }

    public void testInstalledPluginCanDependOnBundledPlugin() throws Exception {
        PluginJarBuilder bundledJar = new PluginJarBuilder("bundled")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Export-Package: org.maera.test.bundled",
                        "Bundle-SymbolicName: bundled",
                        "Bundle-Version: 1.0.0",
                        "Manifest-Version: 1.0",
                        "")
                .addFormattedJava("org.maera.test.bundled.BundledInterface",
                        "package org.maera.test.bundled;",
                        "public interface BundledInterface {}");
        bundledJar.build(bundledPluginDir);

        new PluginJarBuilder("installed", bundledJar.getClassLoader())
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Installed Plugin' key='installed' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='installed' class='org.maera.test.installed.InstalledClass'/>",
                        "</maera-plugin>")
                .addFormattedJava("org.maera.test.installed.InstalledClass",
                        "package org.maera.test.installed;",
                        "import org.maera.test.bundled.BundledInterface;",
                        "public class InstalledClass implements BundledInterface {}")
                .build(pluginDir);

        zipBundledPlugins();

        final PluginsConfiguration config = pluginsConfiguration()
                .pluginDirectory(pluginDir)
                .bundledPluginUrl(bundledPluginZip.toURI().toURL())
                .bundledPluginCacheDirectory(bundledPluginDir)
                .packageScannerConfiguration(
                        new PackageScannerConfigurationBuilder()
                                .packagesToInclude("org.maera.*", "org.slf4j", "org.apache.commons.logging")
                                .packagesVersions(Collections.singletonMap("org.apache.commons.logging", "1.1.1"))
                                .build())
                .build();
        plugins = new MaeraPlugins(config);
        plugins.start();
        assertEquals(2, plugins.getPluginAccessor().getEnabledPlugins().size());

    }

    private void zipBundledPlugins() throws IOException {
        ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(bundledPluginZip)));
        for (File bundledPlugin : bundledPluginDir.listFiles()) {
            zip.putNextEntry(new ZipEntry(bundledPlugin.getName()));
            zip.write(FileUtils.readFileToByteArray(bundledPlugin));
        }
        zip.close();
    }
}
