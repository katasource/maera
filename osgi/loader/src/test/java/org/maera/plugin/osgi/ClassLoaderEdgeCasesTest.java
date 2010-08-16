package org.maera.plugin.osgi;

import org.junit.Test;
import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.osgi.factory.OsgiPlugin;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * These tests are mainly here to demonstrate different edge cases you can encounter
 */
public class ClassLoaderEdgeCasesTest extends AbstractPluginInContainerTest {

    @Test
    public void testLinkageError() throws Exception {
        File privateJar = new PluginJarBuilder("private-jar")
                .addFormattedJava("org.maera.plugin.osgi.Callable2",
                        "package org.maera.plugin.osgi;",
                        "public interface Callable2 {",
                        "    String call() throws Exception;",
                        "}")
                .build();

        File pluginJar = new PluginJarBuilder("privatejartest")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.privatejar.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object key='obj' class='my.Foo'/>",
                        "</maera-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "import org.maera.plugin.osgi.Callable2;",
                        "import org.maera.plugin.osgi.test.Callable2Factory;",
                        "public class Foo {",
                        "  public String call() throws Exception { return 'hi ' + new Callable2Factory().create().call();}",
                        "}")
                .addFile("META-INF/lib/private.jar", privateJar)
                .build();
        DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("object", ObjectModuleDescriptor.class);
        initPluginManager(null, factory);
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        OsgiPlugin plugin = (OsgiPlugin) pluginManager.getPlugin("test.privatejar.plugin");
        assertEquals("Test", plugin.getName());
        Class foo = plugin.getModuleDescriptor("obj").getModuleClass();
        Object fooObj = plugin.autowire(foo);
        try {
            Method method = foo.getMethod("call");
            method.invoke(fooObj);
            fail("Should have thrown linkage error");
        }
        catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof LinkageError) {
                // passed
            } else {
                fail("Should have thrown linkage error");
            }
        }
    }
}