package org.maera.plugin.osgi;

import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.osgi.container.felix.FelixOsgiContainerManager;
import org.maera.plugin.osgi.factory.OsgiPlugin;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.test.PluginJarBuilder;
import org.maera.plugin.util.WaitUntil;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.Callable;

public class TestPluginDependencies extends PluginInContainerTestBase {
    public void testPluginDependentOnPackageImport() throws Exception {
        PluginJarBuilder parentBuilder = new PluginJarBuilder("parent")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='parent' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions>",
                        "            <Import-Package>foo</Import-Package>",
                        "            <Export-Package>foo</Export-Package>",
                        "        </bundle-instructions>",
                        "    </plugin-info>",
                        "</maera-plugin>")
                .addFormattedJava("foo.Bar",
                        "package foo;",
                        "public interface Bar {}");

        new PluginJarBuilder("child", parentBuilder.getClassLoader())
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='child' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "</maera-plugin>")
                .addFormattedJava("second.MyImpl",
                        "package second;",
                        "public class MyImpl {",
                        "    public MyImpl(foo.Bar config) {",
                        "    }",
                        "}")
                .build(pluginsDir);

        parentBuilder.build(pluginsDir);
        initPluginManager();
        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertEquals(Collections.singleton("parent"), pluginManager.getPlugin("child").getRequiredPlugins());
    }

    public void testUninstallingPluginDependentOnPackageImport() throws Exception {
        PluginJarBuilder parentBuilder = new PluginJarBuilder("parent")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='parent' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions>",
                        "            <Import-Package>foo</Import-Package>",
                        "            <Export-Package>foo</Export-Package>",
                        "        </bundle-instructions>",
                        "    </plugin-info>",
                        "</maera-plugin>")
                .addFormattedJava("foo.Bar",
                        "package foo;",
                        "public interface Bar {}");

        new PluginJarBuilder("child", parentBuilder.getClassLoader())
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='child' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "</maera-plugin>")
                .addFormattedJava("second.MyImpl",
                        "package second;",
                        "public class MyImpl {",
                        "    public MyImpl(foo.Bar config) {",
                        "    }",
                        "}")
                .build(pluginsDir);

        parentBuilder.build(pluginsDir);
        initPluginManager();
        assertEquals(2, pluginManager.getEnabledPlugins().size());
        pluginManager.uninstall(pluginManager.getPlugin("parent"));
        assertEquals(0, pluginManager.getEnabledPlugins().size());
    }

    public void testUpgradeWithNewComponentImplementation() throws Exception {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface() {
                });
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface() {
                });
            }
        }, factory);

        final File pluginJar = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'hi';}",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("second")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='svc' interface='java.util.concurrent.Callable' />",
                        "    <component key='del' class='my2.ServiceDelegate' public='true'>",
                        "    <interface>org.maera.plugin.osgi.Callable2</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my2.ServiceDelegate",
                        "package my2;",
                        "import org.maera.plugin.osgi.Callable2;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceDelegate implements Callable2 {",
                        "    private final Callable delegate;",
                        "    public ServiceDelegate(Callable foo) { this.delegate = foo;}",
                        "    public String call() throws Exception { return (String)delegate.call();}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        final ServiceTracker tracker = osgiContainerManager.getServiceTracker("org.maera.plugin.osgi.Callable2");

        for (final Object svc : tracker.getServices()) {
            final Callable2 callable = (Callable2) svc;
            assertEquals("hi", callable.call());
        }
        assertEquals(2, pluginManager.getEnabledPlugins().size());

        final File updatedJar = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'bob';}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(updatedJar));
        WaitUntil.invoke(new AbstractWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getEnabledPlugins().size() == 2;
            }
        });
        assertEquals(2, pluginManager.getEnabledPlugins().size());
        for (final Object svc : tracker.getServices()) {
            final Callable2 callable = (Callable2) svc;
            assertEquals("bob", callable.call());
        }
    }

    public void testUpgradeWithNewComponentImplementationWithInterfaceInPlugin() throws Exception {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        factory.addModuleDescriptor("dummy", DummyModuleDescriptor.class);
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
                registrar.register(SomeInterface.class).forInstance(new SomeInterface() {
                });
                registrar.register(AnotherInterface.class).forInstance(new AnotherInterface() {
                });
            }
        }, factory);

        final PluginJarBuilder builder1 = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>my.Service</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.Service",
                        "package my;",
                        "public interface Service {",
                        "    public Object call() throws Exception;",
                        "}")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "public class ServiceImpl implements Service {",
                        "    public Object call() throws Exception { return 'hi';}",
                        "}");
        final File pluginJar = builder1.build();
        final File pluginJar2 = new PluginJarBuilder("second", builder1.getClassLoader())
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component-import key='svc' interface='my.Service' />",
                        "    <component key='del' class='my2.ServiceDelegate' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my2.ServiceDelegate",
                        "package my2;",
                        "import my.Service;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceDelegate implements Callable {",
                        "    private final Service delegate;",
                        "    public ServiceDelegate(Service foo) { this.delegate = foo;}",
                        "    public Object call() throws Exception { return delegate.call();}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());
        final ServiceTracker tracker = osgiContainerManager.getServiceTracker(Callable.class.getName());
        final ServiceTracker svcTracker = osgiContainerManager.getServiceTracker("my.Service");

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        assertEquals("hi", svcTracker.getService().getClass().getMethod("call").invoke(svcTracker.getService()));
        assertEquals("hi", ((Callable) tracker.getService()).call());

        assertEquals(2, pluginManager.getEnabledPlugins().size());

        final File updatedJar = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.Service2Impl' public='true'>",
                        "    <interface>my.Service</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.Service",
                        "package my;",
                        "public interface Service {",
                        "    public Object call() throws Exception;",
                        "}")
                .addFormattedJava("my.Service2Impl",
                        "package my;",
                        "public class Service2Impl implements Service {",
                        "    public Object call() throws Exception {return 'bob';}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(updatedJar));
        assertEquals("bob", svcTracker.getService().getClass().getMethod("call").invoke(svcTracker.getService()));
        tracker.waitForService(5000);
        assertEquals("bob", ((Callable) tracker.getService()).call());
    }

    public void testUpgradeWithRefreshingAffectingOtherPlugins() throws Exception {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        }, factory);

        PluginJarBuilder pluginBuilder = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions><Export-Package>my</Export-Package></bundle-instructions>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'hi';}",
                        "}");
        final File pluginJar = pluginBuilder.build();

        final File pluginJar2 = new PluginJarBuilder("second", pluginBuilder.getClassLoader())
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions><Import-Package>my,*</Import-Package></bundle-instructions>",
                        "    </plugin-info>",
                        "    <component-import key='svc' interface='java.util.concurrent.Callable' />",
                        "    <component-import key='othersvc' interface='org.maera.plugin.osgi.Callable3' />",
                        "    <component key='del' class='my2.ServiceDelegate' public='true'>",
                        "    <interface>org.maera.plugin.osgi.Callable2</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my2.ServiceDelegate",
                        "package my2;",
                        "import org.maera.plugin.osgi.Callable2;",
                        "import org.maera.plugin.osgi.Callable3;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceDelegate implements Callable2 {",
                        "    private final Callable delegate;",
                        "    private final Callable3 othersvc;",
                        "    public ServiceDelegate(Callable foo,Callable3 othersvc) {",
                        "        this.delegate = foo;",
                        "        this.othersvc = othersvc;",
                        "    }",
                        "    public String call() throws Exception { return othersvc.call() + (String)delegate.call();}",
                        "}")
                .build();
        final File otherSvcJar = new PluginJarBuilder("otherSvc")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.othersvc.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='othersvc' class='othersvc.ServiceImpl' public='true'>",
                        "    <interface>org.maera.plugin.osgi.Callable3</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("othersvc.ServiceImpl",
                        "package othersvc;",
                        "import org.maera.plugin.osgi.Callable3;",
                        "public class ServiceImpl implements Callable3 {",
                        "    public String call() throws Exception { return 'hi';}",
                        "}")
                .build();


        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());

        pluginManager.installPlugin(new JarPluginArtifact(otherSvcJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));

        assertEquals("hi", ((OsgiPlugin) pluginManager.getPlugin("test2.plugin")).autowire(TestPluginInstall.Callable3Aware.class).call());
        assertEquals(3, pluginManager.getEnabledPlugins().size());

        final File updatedJar = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions><Export-Package>my</Export-Package></bundle-instructions>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'bob';}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(updatedJar));
        assertEquals(3, pluginManager.getEnabledPlugins().size());
        assertEquals("hi", ((OsgiPlugin) pluginManager.getPlugin("test2.plugin")).autowire(TestPluginInstall.Callable3Aware.class).call());
    }

    public void testUpgradeWithRefreshingAffectingOtherPluginsWithClassLoadingOnShutdown() throws Exception {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        }, factory);

        PluginJarBuilder pluginBuilder = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions><Export-Package>my</Export-Package></bundle-instructions>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'hi';}",
                        "}");
        final File pluginJar = pluginBuilder.build();

        final File pluginJar2 = new PluginJarBuilder("second", pluginBuilder.getClassLoader())
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions><Import-Package>my,*</Import-Package>",
                        "          <DynamicImport-Package>foo</DynamicImport-Package></bundle-instructions>",
                        "    </plugin-info>",
                        "    <component-import key='svc' interface='java.util.concurrent.Callable' />",
                        "    <component-import key='othersvc' interface='org.maera.plugin.osgi.Callable3' />",
                        "    <component key='del' class='my2.ServiceDelegate' public='true'>",
                        "    <interface>org.maera.plugin.osgi.Callable2</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my2.ServiceDelegate",
                        "package my2;",
                        "import org.maera.plugin.osgi.Callable2;",
                        "import org.maera.plugin.osgi.Callable3;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceDelegate implements Callable2, org.springframework.beans.factory.DisposableBean {",
                        "    private final Callable delegate;",
                        "    private final Callable3 othersvc;",
                        "    public ServiceDelegate(Callable foo,Callable3 othersvc) {",
                        "        this.delegate = foo;",
                        "        this.othersvc = othersvc;",
                        "    }",
                        "    public void destroy() {",
                        "       try {",
                        "          getClass().getClassLoader().loadClass('foo.bar');",
                        "       } catch (ClassNotFoundException ex) {}",
                        "    }",
                        "    public String call() throws Exception { return othersvc.call() + (String)delegate.call();}",
                        "}")
                .build();
        final File otherSvcJar = new PluginJarBuilder("otherSvc")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.othersvc.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='othersvc' class='othersvc.ServiceImpl' public='true'>",
                        "    <interface>org.maera.plugin.osgi.Callable3</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("othersvc.ServiceImpl",
                        "package othersvc;",
                        "import org.maera.plugin.osgi.Callable3;",
                        "public class ServiceImpl implements Callable3 {",
                        "    public String call() throws Exception { return 'hi';}",
                        "}")
                .build();


        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());

        pluginManager.installPlugin(new JarPluginArtifact(otherSvcJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));

        assertEquals("hi", ((OsgiPlugin) pluginManager.getPlugin("test2.plugin")).autowire(TestPluginInstall.Callable3Aware.class).call());
        assertEquals(3, pluginManager.getEnabledPlugins().size());

        final File updatedJar = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions><Export-Package>my</Export-Package></bundle-instructions>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'bob';}",
                        "}")
                .build();

        long start = System.currentTimeMillis();
        pluginManager.installPlugin(new JarPluginArtifact(updatedJar));
        long timeWaitingForRefresh = System.currentTimeMillis() - start;
        assertTrue("Refresh seemed to have timed out, which is bad", timeWaitingForRefresh < FelixOsgiContainerManager.REFRESH_TIMEOUT * 1000);
        assertEquals(3, pluginManager.getEnabledPlugins().size());
        assertEquals("hi", ((OsgiPlugin) pluginManager.getPlugin("test2.plugin")).autowire(TestPluginInstall.Callable3Aware.class).call());
    }

    public void testUninstallWithShutdownAffectingOtherPluginsWithClassLoadingOnShutdown() throws Exception {
        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(new DefaultHostContainer());
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        }, factory);

        PluginJarBuilder pluginBuilder = new PluginJarBuilder("first")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions><Export-Package>my</Export-Package></bundle-instructions>",
                        "    </plugin-info>",
                        "    <component key='svc' class='my.ServiceImpl' public='true'>",
                        "    <interface>java.util.concurrent.Callable</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("my.ServiceImpl",
                        "package my;",
                        "import java.util.concurrent.Callable;",
                        "public class ServiceImpl implements Callable {",
                        "    public Object call() throws Exception { return 'hi';}",
                        "}");
        final File pluginJar = pluginBuilder.build();

        final File pluginJar2 = new PluginJarBuilder("second", pluginBuilder.getClassLoader())
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test2.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "        <bundle-instructions><Import-Package>my,*</Import-Package>",
                        "          <DynamicImport-Package>foo</DynamicImport-Package></bundle-instructions>",
                        "    </plugin-info>",
                        "    <component-import key='svc' interface='java.util.concurrent.Callable' />",
                        "    <component key='del' class='my2.Consumer'/>",
                        "</maera-plugin>")
                .addFormattedJava("my2.Consumer",
                        "package my2;",
                        "import java.util.concurrent.Callable;",
                        "public class Consumer implements org.springframework.beans.factory.DisposableBean {",
                        "    private final Callable delegate;",
                        "    public Consumer(Callable foo) {",
                        "        this.delegate = foo;",
                        "    }",
                        "    public void destroy() {",
                        "       try {",
                        "          getClass().getClassLoader().loadClass('foo.bar');",
                        "       } catch (ClassNotFoundException ex) {}",
                        "    }",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertEquals(1, pluginManager.getEnabledPlugins().size());

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));

        assertEquals(2, pluginManager.getEnabledPlugins().size());

        long start = System.currentTimeMillis();
        pluginManager.uninstall(pluginManager.getPlugin("test.plugin"));
        long timeWaitingForRefresh = System.currentTimeMillis() - start;
        assertTrue("Refresh seemed to have timed out, which is bad", timeWaitingForRefresh < FelixOsgiContainerManager.REFRESH_TIMEOUT * 1000);
        assertEquals(0, pluginManager.getEnabledPlugins().size());
    }
}