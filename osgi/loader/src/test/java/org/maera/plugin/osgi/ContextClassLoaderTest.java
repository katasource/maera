package org.maera.plugin.osgi;

import junit.framework.TestCase;
import org.junit.Test;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;

import static org.junit.Assert.*;

public class ContextClassLoaderTest extends AbstractPluginInContainerTest {

    @Test
    public void testCorrectContextClassLoaderForHostComponents() throws Exception {
        final DummyHostComponentImpl comp = new DummyHostComponentImpl(TestCase.class.getName());
        File plugin = new PluginJarBuilder("ccltest")
                .addResource("maera-plugin.xml",
                        "<maera-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                                "    <plugin-info>\n" +
                                "        <version>1.0</version>\n" +
                                "    </plugin-info>\n" +
                                "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                                "</maera-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import org.maera.plugin.osgi.DummyHostComponent;" +
                        "public class FooImpl implements Foo {public FooImpl(DummyHostComponent comp) throws Exception { comp.evaluate(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider() {

            public void provide(ComponentRegistrar registrar) {
                registrar.register(DummyHostComponent.class).forInstance(comp).withName("hostComp");
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(plugin));

        assertNotNull(comp.cl);
        assertNotNull(comp.testClass);
        assertTrue(comp.testClass == TestCase.class);
    }

    @Test
    public void testCorrectContextClassLoaderForHostComponentsUseHostStrategy() throws Exception {
        final DummyHostComponentImpl comp = new DummyHostComponentImpl(TestCase.class.getName());
        File plugin = new PluginJarBuilder("ccltest")
                .addResource("maera-plugin.xml",
                        "<maera-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                                "    <plugin-info>\n" +
                                "        <version>1.0</version>\n" +
                                "    </plugin-info>\n" +
                                "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                                "</maera-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import org.maera.plugin.osgi.DummyHostComponent;" +
                        "public class FooImpl implements Foo {public FooImpl(DummyHostComponent comp) throws Exception { comp.evaluate(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider() {

            public void provide(ComponentRegistrar registrar) {
                registrar.register(DummyHostComponent.class).forInstance(comp).withName("hostComp").withContextClassLoaderStrategy(ContextClassLoaderStrategy.USE_HOST);
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(plugin));

        assertNotNull(comp.cl);
        assertNotNull(comp.testClass);
        assertTrue(comp.testClass == TestCase.class);
    }

    @Test
    public void testCorrectContextClassLoaderForHostComponentsUsePluginStrategy() throws Exception {
        final DummyHostComponentImpl comp = new DummyHostComponentImpl(TestCase.class.getName());
        File plugin = new PluginJarBuilder("ccltest")
                .addResource("maera-plugin.xml",
                        "<maera-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                                "    <plugin-info>\n" +
                                "        <version>1.0</version>\n" +
                                "    </plugin-info>\n" +
                                "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                                "</maera-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import org.maera.plugin.osgi.DummyHostComponent;" +
                        "public class FooImpl implements Foo {public FooImpl(DummyHostComponent comp) throws Exception { comp.evaluate(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider() {

            public void provide(ComponentRegistrar registrar) {
                registrar.register(DummyHostComponent.class).forInstance(comp).withName("hostComp").withContextClassLoaderStrategy(ContextClassLoaderStrategy.USE_PLUGIN);
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(plugin));

        assertNotNull(comp.cl);
        assertNull(comp.testClass);
    }

    @Test
    public void testCorrectContextClassLoaderForHostComponentsUsePluginStrategyLoadingLocalClass() throws Exception {
        final DummyHostComponentImpl comp = new DummyHostComponentImpl("my.Foo");
        File plugin = new PluginJarBuilder("ccltest")
                .addResource("maera-plugin.xml",
                        "<maera-plugin key=\"ccltest\" pluginsVersion=\"2\">\n" +
                                "    <plugin-info>\n" +
                                "        <version>1.0</version>\n" +
                                "    </plugin-info>\n" +
                                "    <component key=\"foo\" class=\"my.FooImpl\" />\n" +
                                "</maera-plugin>")
                .addJava("my.Foo", "package my;public interface Foo {}")
                .addJava("my.FooImpl", "package my;import org.maera.plugin.osgi.DummyHostComponent;" +
                        "public class FooImpl implements Foo {public FooImpl(DummyHostComponent comp) throws Exception { comp.evaluate(); }}")
                .build();
        HostComponentProvider prov = new HostComponentProvider() {

            public void provide(ComponentRegistrar registrar) {
                registrar.register(DummyHostComponent.class).forInstance(comp).withName("hostComp").withContextClassLoaderStrategy(ContextClassLoaderStrategy.USE_PLUGIN);
            }
        };

        initPluginManager(prov);
        pluginManager.installPlugin(new JarPluginArtifact(plugin));

        assertNotNull(comp.cl);
        assertNotNull(comp.testClass);
    }

    public static class DummyHostComponentImpl implements DummyHostComponent {

        public ClassLoader cl;
        public Class testClass;
        private String classToLoad;

        public DummyHostComponentImpl(String classToLoad) {
            this.classToLoad = classToLoad;
        }

        public void evaluate() {
            cl = Thread.currentThread().getContextClassLoader();
            try {
                testClass = cl.loadClass(classToLoad);
            } catch (ClassNotFoundException ignored) {

            }
        }
    }
}
