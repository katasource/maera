package org.maera.plugin.osgi;

import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.test.PluginJarBuilder;

import java.util.concurrent.Callable;

public class TestPluginFrameworkWarmRestart extends PluginInContainerTestBase {
    public void testWarmRestart() throws Exception {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(hostContainer);
        factory.addModuleDescriptor("object", CallableModuleDescriptor.class);

        final HostComponentProvider prov = new HostComponentProvider() {
            private int count = 1;

            public void provide(ComponentRegistrar registrar) {
                registrar.register(Callable.class).forInstance(new Callable() {
                    public Object call() {
                        return "count:" + (count++) + "-";
                    }
                });
            }
        };

        new PluginJarBuilder("testWarmRestart")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object key='obj' class='my.Foo'/>",
                        "    <object key='obj-disabled' class='my.Foo'/>",
                        "</atlassian-plugin>")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class Foo implements Callable {",
                        "   private Callable host;",
                        "   public Foo(Callable host) { this.host = host; }",
                        "   public Object call() throws Exception { return ((String)host.call()) + System.identityHashCode(this); }",
                        "}")
                .build(pluginsDir);
        initPluginManager(prov, factory);
        pluginManager.disablePluginModule("test.plugin:obj-disabled");

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals(1, pluginManager.getEnabledModulesByClass(Callable.class).size());
        assertEquals("Test", pluginManager.getPlugin("test.plugin").getName());
        assertEquals("my.Foo", pluginManager.getPlugin("test.plugin").getModuleDescriptor("obj").getModule().getClass().getName());
        String value = (String) pluginManager.getEnabledModulesByClass(Callable.class).get(0).call();
        assertTrue(value.startsWith("count:1-"));
        String id = value.substring("count:1-".length());

        pluginManager.warmRestart();

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals(1, pluginManager.getEnabledModulesByClass(Callable.class).size());
        assertEquals("Test", pluginManager.getPlugin("test.plugin").getName());
        assertEquals("my.Foo", pluginManager.getPlugin("test.plugin").getModuleDescriptor("obj").getModule().getClass().getName());
        String value2 = (String) pluginManager.getEnabledModulesByClass(Callable.class).get(0).call();
        assertTrue(value2.startsWith("count:2-"));
        String id2 = value2.substring("count:2-".length());

        assertNotSame(id, id2);

    }
}
