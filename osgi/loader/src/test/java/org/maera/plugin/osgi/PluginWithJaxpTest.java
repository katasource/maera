package org.maera.plugin.osgi;

import org.junit.Test;
import org.maera.plugin.test.PluginJarBuilder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PluginWithJaxpTest extends AbstractPluginInContainerTest {

    @Test
    public void testDisposable() throws Exception {
        StaticBooleanFlag.flag.set(false);
        new PluginJarBuilder("testDisposable")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='obj' class='my.Foo'/>",
                        "</maera-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo implements org.springframework.beans.factory.DisposableBean{",
                        "  public void destroy() {",
                        "    org.maera.plugin.osgi.StaticBooleanFlag.flag.set(true);",
                        "  }",
                        "}")
                .build(pluginsDir);

        initPluginManager();
        assertFalse(StaticBooleanFlag.flag.get());

        // on disable
        pluginManager.disablePlugin("test.plugin");
        assertTrue(StaticBooleanFlag.flag.get());
        pluginManager.enablePlugin("test.plugin");

        // on framework shutdown
        StaticBooleanFlag.flag.set(false);
        osgiContainerManager.stop();
        assertTrue(StaticBooleanFlag.flag.get());
    }
}
