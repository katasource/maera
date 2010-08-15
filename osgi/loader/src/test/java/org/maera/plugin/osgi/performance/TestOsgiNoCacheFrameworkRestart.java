package org.maera.plugin.osgi.performance;

import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.util.Random;

/**
 * Tests the plugin framework handling restarts correctly
 */
public class TestOsgiNoCacheFrameworkRestart extends FrameworkRestartTestBase {
    private final Random rnd = new Random(System.currentTimeMillis());

    protected void addPlugin(File dir, int pluginId) throws Exception {
        System.out.println("building plugin " + pluginId);
        PluginJarBuilder builder = new PluginJarBuilder("restart-test", null);

        StringBuilder apxml = new StringBuilder();
        apxml.append("<atlassian-plugin name='Test' key='test.plugin" + pluginId + "' pluginsVersion='2'>\n" +
                "    <plugin-info>\n" +
                "        <version>1.0</version>\n" +
                "    </plugin-info>\n" +
                //"    <component-import key='host1' interface='org.maera.plugin.osgi.SomeInterface' />\n" +
                "    <dummy key='dum1'/>\n");

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
            apxml.append("  <component key='comp" + x + "' interface='" + pkg + ".MyInterface' class='" + pkg + ".MyComponent' ");
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

                apxml.append("  <component-import key='ref" + x + "' interface='" + pkg(refid, x) + ".MyInterface'/>\n");
            }
        }
        apxml.append("</atlassian-plugin>");
        builder.addFormattedResource("atlassian-plugin.xml", apxml.toString());
        builder.build(dir);
        System.out.println("plugin " + pluginId + " built");
    }

    private String pkg(int pluginId, int x) {
        return "plugin" + pluginId + ".component" + x;
    }
}