package org.maera.plugin.osgi;

import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.Plugin;
import org.maera.plugin.hostcontainer.HostContainer;
import org.maera.plugin.impl.UnloadablePlugin;
import org.maera.plugin.loaders.ClassPathPluginLoader;
import org.maera.plugin.module.ClassPrefixModuleFactory;
import org.maera.plugin.module.PrefixDelegatingModuleFactory;
import org.maera.plugin.module.PrefixModuleFactory;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.osgi.module.BeanPrefixModuleFactory;
import org.maera.plugin.osgi.test.TestServlet;
import org.maera.plugin.servlet.ServletModuleManager;
import org.maera.plugin.servlet.descriptors.ServletModuleDescriptor;
import org.maera.plugin.test.PluginJarBuilder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.HashSet;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

/**
 * Tests around the creation of the module class of {@link org.maera.plugin.ModuleDescriptor}
 */
public class TestPluginModuleCreation extends PluginInContainerTestBase {
    public void testInstallPlugin2AndGetModuleClass() throws Exception {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar = firstBuilder
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <servlet key='foo' class='first.MyServlet'>",
                        "       <url-pattern>/foo</url-pattern>",
                        "    </servlet>",
                        "</maera-plugin>")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();

        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);

        HostContainer hostContainer = mock(HostContainer.class);
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleFactory, servletModuleManager));

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        }, moduleDescriptorFactory);

        pluginManager.installPlugin(new JarPluginArtifact(jar));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("first.MyServlet", pluginManager.getPlugin("first").getModuleDescriptor("foo").getModule().getClass().getName());
    }

    public void testInstallPlugins1AndGetModuleClass() throws Exception {
        ClassPathPluginLoader classPathPluginLoader = new ClassPathPluginLoader("testInstallPlugins1AndGetModuleClass.xml");
        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        final HostContainer hostContainer = mock(HostContainer.class);
        moduleFactory = new PrefixDelegatingModuleFactory(new HashSet<PrefixModuleFactory>() {{
            add(new ClassPrefixModuleFactory(hostContainer));
            add(new BeanPrefixModuleFactory());
        }});
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleFactory, servletModuleManager));
        when(hostContainer.create(TestServlet.class)).thenReturn(new TestServlet());

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(moduleDescriptorFactory, classPathPluginLoader);

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("org.maera.plugin.osgi.test.TestServlet", pluginManager.getPlugin("first").getModuleDescriptor("foo").getModule().getClass().getName());
    }

    public void testInstallPlugins1AndFailToGetModuleClassFromSpring() throws Exception {
        ClassPathPluginLoader classPathPluginLoader = new ClassPathPluginLoader("testInstallPlugins1AndFailToGetModuleClassFromSpring.xml");
        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);

        final HostContainer hostContainer = mock(HostContainer.class);
        moduleFactory = new PrefixDelegatingModuleFactory(new HashSet<PrefixModuleFactory>() {{
            add(new ClassPrefixModuleFactory(hostContainer));
            add(new BeanPrefixModuleFactory());
        }});
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleFactory, servletModuleManager));
        when(hostContainer.create(TestServlet.class)).thenReturn(new TestServlet());
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ServletModuleDescriptor) invocation.getArguments()[0]).getModule();
                return null;
            }
        }).when(servletModuleManager).addServletModule((ServletModuleDescriptor) anyObject());

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(moduleDescriptorFactory, classPathPluginLoader);
        assertEquals(1, pluginManager.getPlugins().size());
        final Plugin plugin = pluginManager.getPlugins().iterator().next();
        assertTrue(plugin instanceof UnloadablePlugin);
        assertEquals(0, pluginManager.getEnabledPlugins().size());
    }

    public void testInstallPlugins2AndGetModuleClassFromSpring() throws Exception {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar = firstBuilder
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <servlet key='foo' class='bean:obj'>",
                        "       <url-pattern>/foo</url-pattern>",
                        "    </servlet>",
                        "<component key='obj' class='first.MyServlet'/>",
                        "</maera-plugin>")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();

        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        HostContainer hostContainer = mock(HostContainer.class);
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleFactory, servletModuleManager));

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        }, moduleDescriptorFactory);

        pluginManager.installPlugin(new JarPluginArtifact(jar));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("first.MyServlet", pluginManager.getPlugin("first").getModuleDescriptor("foo").getModule().getClass().getName());
    }

    public void testGetModuleClassFromComponentModuleDescriptor() throws Exception {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar = firstBuilder
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "<component key='obj' class='first.MyServlet'/>",
                        "</maera-plugin>")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();


        initPluginManager();

        pluginManager.installPlugin(new JarPluginArtifact(jar));

        assertEquals(1, pluginManager.getEnabledPlugins().size());
        assertEquals("first.MyServlet", pluginManager.getPlugin("first").getModuleDescriptor("obj").getModule().getClass().getName());
    }

    public void testGetModuleClassFromComponentImportModuleDescriptor() throws Exception {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar1 = firstBuilder
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test1' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "<component key='obj' class='first.MyServlet' public='true'>",
                        "<interface>org.maera.plugin.osgi.SomeInterface</interface>",
                        "</component>",
                        "</maera-plugin>")
                .addFormattedJava("org.maera.plugin.osgi.SomeInterface",
                        "package org.maera.plugin.osgi;",
                        "public interface SomeInterface {}")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet implements org.maera.plugin.osgi.SomeInterface {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();

        final File jar2 = new PluginJarBuilder("second")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test2' key='second' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='obj' interface='org.maera.plugin.osgi.SomeInterface' />",
                        "</maera-plugin>"
                )
                .addFormattedJava("org.maera.plugin.osgi.SomeInterface",
                        "package org.maera.plugin.osgi;",
                        "public interface SomeInterface {}")
                .build();

        initPluginManager();
        pluginManager.installPlugin(new JarPluginArtifact(jar1));
        pluginManager.installPlugin(new JarPluginArtifact(jar2));


        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertEquals("first.MyServlet", pluginManager.getPlugin("first").getModuleDescriptor("obj").getModule().getClass().getName());
        assertTrue(pluginManager.getPlugin("second").getModuleDescriptor("obj").getModule() instanceof SomeInterface);
    }

    public void testFailToGetModuleClassFromSpring() throws Exception {
        final PluginJarBuilder firstBuilder = new PluginJarBuilder("first");
        final File jar = firstBuilder
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='first' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <servlet key='foo' class='bean:beanId' name='spring bean for servlet'>",
                        "       <url-pattern>/foo</url-pattern>",
                        "    </servlet>",
                        "<component key='obj' class='first.MyServlet' />",
                        "</maera-plugin>")
                .addFormattedJava("first.MyServlet",
                        "package first;",
                        "import javax.servlet.http.HttpServlet;",
                        "public class MyServlet extends javax.servlet.http.HttpServlet {",
                        "   public String getServletInfo() {",
                        "       return 'bob';",
                        "   }",
                        "}")
                .build();

        final ServletModuleManager servletModuleManager = mock(ServletModuleManager.class);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ((ServletModuleDescriptor) invocation.getArguments()[0]).getModule();
                return null;
            }
        }).when(servletModuleManager).addServletModule((ServletModuleDescriptor) anyObject());
        final HostContainer hostContainer = mock(HostContainer.class);
        moduleFactory = new PrefixDelegatingModuleFactory(new HashSet<PrefixModuleFactory>() {{
            add(new ClassPrefixModuleFactory(hostContainer));
            add(new BeanPrefixModuleFactory());
        }});
        when(hostContainer.create(StubServletModuleDescriptor.class)).thenReturn(new StubServletModuleDescriptor(moduleFactory, servletModuleManager));

        final DefaultModuleDescriptorFactory moduleDescriptorFactory = new DefaultModuleDescriptorFactory(hostContainer);
        moduleDescriptorFactory.addModuleDescriptor("servlet", StubServletModuleDescriptor.class);

        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        }, moduleDescriptorFactory);

        pluginManager.installPlugin(new JarPluginArtifact(jar));
        assertEquals(0, pluginManager.getEnabledPlugins().size());
        final Plugin plugin = pluginManager.getPlugins().iterator().next();
        assertTrue(plugin instanceof UnloadablePlugin);
    }
}
