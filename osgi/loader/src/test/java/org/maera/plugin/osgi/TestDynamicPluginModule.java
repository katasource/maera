package org.maera.plugin.osgi;

import org.maera.plugin.DefaultModuleDescriptorFactory;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.descriptors.UnrecognisedModuleDescriptor;
import org.maera.plugin.event.PluginEventListener;
import org.maera.plugin.event.events.PluginModuleDisabledEvent;
import org.maera.plugin.event.events.PluginModuleEnabledEvent;
import org.maera.plugin.osgi.factory.OsgiPlugin;
import org.maera.plugin.osgi.hostcomponents.ComponentRegistrar;
import org.maera.plugin.osgi.hostcomponents.HostComponentProvider;
import org.maera.plugin.test.PluginJarBuilder;
import org.maera.plugin.util.WaitUntil;
import org.maera.plugin.web.descriptors.WebItemModuleDescriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestDynamicPluginModule extends PluginInContainerTestBase {
    public void testDynamicPluginModule() throws Exception {
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='factory' class='foo.MyModuleDescriptorFactory' public='true'>",
                        "       <interface>org.maera.plugin.ModuleDescriptorFactory</interface>",
                        "    </component>",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .addFormattedJava("foo.MyModuleDescriptorFactory",
                        "package foo;",
                        "public class MyModuleDescriptorFactory extends org.maera.plugin.DefaultModuleDescriptorFactory {",
                        "  public MyModuleDescriptorFactory() {",
                        "    super();",
                        "    addModuleDescriptor('foo', MyModuleDescriptor.class);",
                        "  }",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorWithReinstall() throws Exception {
        initPluginManager();

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });

        // uninstall the module - the test plugin modules should revert back to Unrecognised
        pluginManager.uninstall(pluginManager.getPlugin("test.plugin.module"));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                ModuleDescriptor<?> descriptor = pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next();
                boolean enabled = pluginManager.isPluginModuleEnabled(descriptor.getCompleteKey());
                return descriptor
                        .getClass()
                        .getSimpleName()
                        .equals("UnrecognisedModuleDescriptor")
                        && !enabled;
            }
        });
        // reinstall the module - the test plugin modules should be correct again
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorWithImmediateReinstall() throws Exception {
        initPluginManager();

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });

        // reinstall the module - the test plugin modules should be correct again
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
    }

    public void testUpgradeOfBundledPluginWithDynamicModule() throws Exception {
        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();

        final DefaultModuleDescriptorFactory factory = new DefaultModuleDescriptorFactory(hostContainer);
        initBundlingPluginManager(factory, pluginJar);
        assertEquals(1, pluginManager.getEnabledPlugins().size());

        final File pluginClientOld = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();
        final File pluginClientNew = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>2.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();
        pluginManager.installPlugins(new JarPluginArtifact(pluginClientOld), new JarPluginArtifact(pluginClientNew));

        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });

        assertEquals(2, pluginManager.getEnabledPlugins().size());
        assertEquals("2.0", pluginManager.getPlugin("test.plugin").getPluginInformation().getVersion());
    }

    public void testDynamicPluginModuleNotLinkToAllPlugins() throws Exception {
        new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor'/>",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build(pluginsDir);
        new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build(pluginsDir);
        new PluginJarBuilder("foootherUser")
                .addPluginInformation("unusing.plugin", "Unusing plugin", "1.0")
                .build(pluginsDir);

        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        });

        assertEquals("MyModuleDescriptor", pluginManager.getPlugin("test.plugin").getModuleDescriptor("dum2").getClass().getSimpleName());
        Set<String> deps = findDependentBundles(((OsgiPlugin) pluginManager.getPlugin("test.plugin.module")).getBundle());
        assertTrue(deps.contains("test.plugin"));
        assertFalse(deps.contains("unusing.plugin"));
    }

    private Set<String> findDependentBundles(Bundle bundle) {
        Set<String> deps = new HashSet<String>();
        final ServiceReference[] registeredServices = bundle.getRegisteredServices();
        if (registeredServices == null) {
            return deps;
        }

        for (final ServiceReference serviceReference : registeredServices) {
            final Bundle[] usingBundles = serviceReference.getUsingBundles();
            if (usingBundles == null) {
                continue;
            }
            for (final Bundle usingBundle : usingBundles) {
                deps.add(usingBundle.getSymbolicName());
            }
        }
        return deps;
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptor() throws Exception {
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptor("dum2")
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }


    public void testDynamicPluginModuleUsingModuleTypeDescriptorAndComponentInjection() throws Exception {
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <component key='comp' class='foo.MyComponent' />",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyComponent",
                        "package foo;",
                        "public class MyComponent {",
                        "}")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public MyModuleDescriptor(MyComponent comp) {}",
                        "  public Object getModule(){return null;}",
                        "}")

                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorAfterTheFact() throws Exception {
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });

        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());

        pluginManager.uninstall(pluginManager.getPlugin("test.plugin.module"));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptors()
                        .iterator()
                        .next()
                        .getClass()
                        .getSimpleName()
                        .equals("UnrecognisedModuleDescriptor");
            }
        });
        descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        descriptor = descriptors.iterator()
                .next();
        assertEquals("UnrecognisedModuleDescriptor", descriptor.getClass().getSimpleName());

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        descriptor = descriptors.iterator()
                .next();
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorAfterTheFactWithException() throws Exception {
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin.module' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public MyModuleDescriptor() {",
                        "    throw new RuntimeException('error loading module');",
                        "  }",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();
        final File pluginJar2 = new PluginJarBuilder("fooUser")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test 2' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='dum2'/>",
                        "</maera-plugin>")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        assertTrue(WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                UnrecognisedModuleDescriptor des = (UnrecognisedModuleDescriptor) pluginManager.getPlugin("test.plugin").getModuleDescriptor("dum2");
                return des.getErrorText().contains("error loading module");
            }
        }));

    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorInSamePlugin() throws Exception {
        initPluginManager(new HostComponentProvider() {
            public void provide(final ComponentRegistrar registrar) {
            }
        });

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "    <foo key='dum2' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptor("dum2")
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(2, descriptors.size());
        final ModuleDescriptor<?> descriptor = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptor("dum2");
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());
    }

    public void testDynamicPluginModuleUsingModuleTypeDescriptorInSamePluginWithRestart() throws Exception {
        initPluginManager();

        final File pluginJar = new PluginJarBuilder("pluginType")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='test.plugin' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <module-type key='foo' class='foo.MyModuleDescriptor' />",
                        "    <foo key='dum2' />",
                        "</maera-plugin>")
                .addFormattedJava("foo.MyModuleDescriptor",
                        "package foo;",
                        "public class MyModuleDescriptor extends org.maera.plugin.descriptors.AbstractModuleDescriptor {",
                        "  public Object getModule(){return null;}",
                        "}")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptor("dum2")
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(2, descriptors.size());
        ModuleDescriptor<?> descriptor = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptor("dum2");
        assertEquals("MyModuleDescriptor", descriptor.getClass().getSimpleName());

        PluginModuleDisabledListener disabledListener = new PluginModuleDisabledListener();
        PluginModuleEnabledListener enabledListener = new PluginModuleEnabledListener();
        pluginEventManager.register(disabledListener);
        pluginEventManager.register(enabledListener);

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        WaitUntil.invoke(new BasicWaitCondition() {
            public boolean isFinished() {
                return pluginManager.getPlugin("test.plugin")
                        .getModuleDescriptor("dum2")
                        .getClass()
                        .getSimpleName()
                        .equals("MyModuleDescriptor");
            }
        });
        descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(2, descriptors.size());
        ModuleDescriptor<?> newdescriptor = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptor("dum2");
        assertEquals("MyModuleDescriptor", newdescriptor.getClass().getSimpleName());
        assertTrue(descriptor.getClass() != newdescriptor.getClass());
        assertTrue(disabledListener.called);
        assertTrue(enabledListener.called);
    }

    public void testDynamicModuleDescriptor() throws Exception {
        initPluginManager(null);

        final File pluginJar = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin", "foo", "1.0")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        final BundleContext ctx = ((OsgiPlugin) pluginManager.getPlugin("test.plugin")).getBundle()
                .getBundleContext();
        final ServiceRegistration reg = ctx.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        final Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("DummyWebItemModuleDescriptor", descriptor.getClass().getSimpleName());
        List<WebItemModuleDescriptor> list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(1, list.size());
        reg.unregister();
        list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(0, list.size());
    }

    public void testDynamicModuleDescriptorIsolatedToPlugin() throws Exception {
        initPluginManager(null);

        final File pluginJar = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin", "foo", "1.0")
                .build();

        pluginManager.installPlugin(new JarPluginArtifact(pluginJar));
        final BundleContext ctx = ((OsgiPlugin) pluginManager.getPlugin("test.plugin")).getBundle()
                .getBundleContext();
        ctx.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        final File pluginJar2 = new PluginJarBuilder("pluginType").addPluginInformation("test.plugin2", "foo", "1.0")
                .build();
        pluginManager.installPlugin(new JarPluginArtifact(pluginJar2));
        final BundleContext ctx2 = ((OsgiPlugin) pluginManager.getPlugin("test.plugin2")).getBundle()
                .getBundleContext();
        final ServiceRegistration reg2 = ctx2.registerService(ModuleDescriptor.class.getName(), new DummyWebItemModuleDescriptor(), null);

        Collection<ModuleDescriptor<?>> descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
        final ModuleDescriptor<?> descriptor = descriptors.iterator()
                .next();
        assertEquals("DummyWebItemModuleDescriptor", descriptor.getClass().getSimpleName());
        List<WebItemModuleDescriptor> list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(2, list.size());
        reg2.unregister();
        list = pluginManager.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
        assertEquals(1, list.size());
        descriptors = pluginManager.getPlugin("test.plugin")
                .getModuleDescriptors();
        assertEquals(1, descriptors.size());
    }

    public static class PluginModuleEnabledListener {
        public volatile boolean called;

        @PluginEventListener
        public void onEnable(PluginModuleEnabledEvent event) {
            called = true;
        }
    }

    public static class PluginModuleDisabledListener {
        public volatile boolean called;

        @PluginEventListener
        public void onDisable(PluginModuleDisabledEvent event) {
            called = true;
        }
    }
}
