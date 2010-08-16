package org.maera.plugin.osgi.performance;

import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.util.Random;

/**
 * Tests the plugin framework handling restarts correctly
 */
public class OsgiNoCacheFrameworkRestartTest extends AbstractFrameworkRestartTest {
    private final Random rnd = new Random(System.currentTimeMillis());

    protected void addPlugin(File dir, int pluginId) throws Exception {
        System.out.println("building plugin " + pluginId);
        PluginJarBuilder builder = new PluginJarBuilder("restart-test", null);

        StringBuilder apxml = new StringBuilder();
        apxml.append("<maera-plugin name='Test' key='test.plugin")
                .append(pluginId)
                .append("' pluginsVersion='2'>\n")
                .append("    <plugin-info>\n")
                .append("        <version>1.0</version>\n")
                .append("    </plugin-info>\n")
                .append("    <dummy key='dum1'/>\n");

        for (int x = 0; x < 50; x++) {
            String pkg = pkg(pluginId, x);
            builder.addFormattedJava(pkg + ".MyInterface",
                    "package " + pkg + ";",
                    "public interface MyInterface {}");
            builder.addFormattedJava(pkg + ".MyComponent",
                    "package " + pkg + ";",
                    "public class MyComponent implements MyInterface {",
                    "   public MyComponent() {}",
                    "}");
            apxml.append("  <component key='comp")
                    .append(x)
                    .append("' interface='")
                    .append(pkg)
                    .append(".MyInterface' class='")
                    .append(pkg)
                    .append(".MyComponent' ");
            if (x < 10) {
                apxml.append("public='true'");
            }
            apxml.append("/>\n");
        }
        if (pluginId != 49) {

            for (int x = 0; x < 10; x++) {
                int refid;
                do {
                    refid = 50 - rnd.nextInt(50 - pluginId) - 1;
                }
                while (refid == pluginId);

                apxml.append("  <component-import key='ref")
                        .append(x)
                        .append("' interface='")
                        .append(pkg(refid, x))
                        .append(".MyInterface'/>\n");
            }
        }
        apxml.append("</maera-plugin>");
        builder.addFormattedResource("maera-plugin.xml", apxml.toString());
        builder.build(dir);
        System.out.println("plugin " + pluginId + " built");
    }

    private String pkg(int pluginId, int x) {
        return "plugin" + pluginId + ".component" + x;
    }
}