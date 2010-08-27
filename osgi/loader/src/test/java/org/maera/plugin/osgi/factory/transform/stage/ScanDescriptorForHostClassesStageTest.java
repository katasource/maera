package org.maera.plugin.osgi.factory.transform.stage;

import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.osgi.SomeInterface;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.model.SystemExports;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.test.PluginJarBuilder;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanDescriptorForHostClassesStageTest {

    private OsgiContainerManager osgiContainerManager;
    private HostComponentRegistration registration;

    @Before
    public void setUp() throws Exception {
        osgiContainerManager = mock(OsgiContainerManager.class);
        registration = mock(HostComponentRegistration.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
    }

    @Test
    public void testTransform() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='bar' class='org.maera.plugin.osgi.Foo' />",
                        "</maera-plugin>")
                .build();

        ScanDescriptorForHostClassesStage stage = new ScanDescriptorForHostClassesStage();
        SystemExports exports = new SystemExports("org.maera.plugin.osgi");
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration>emptyList(), exports, new JarPluginArtifact(plugin),
                null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertTrue(context.getExtraImports().contains("org.maera.plugin.osgi"));
    }

    @Test
    public void testTransformButPackageInPlugin() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='bar' class='org.maera.plugin.osgi.Foo' />",
                        "</maera-plugin>")
                .addResource("org/maera/plugin/osgi/", "")
                .addResource("org/maera/plugin/osgi/Foo.class", "asd")
                .build();

        ScanDescriptorForHostClassesStage stage = new ScanDescriptorForHostClassesStage();
        SystemExports exports = new SystemExports("org.maera.plugin.osgi");
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration>emptyList(), exports, new JarPluginArtifact(plugin),
                null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertTrue(!context.getExtraImports().contains("org.maera.plugin.osgi"));
    }

    @Test
    public void testTransformIgnoreUnknown() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test Bundle instruction plugin 2' key='test.plugin'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <foo key='bar' class='blat.Foo' />",
                        "</maera-plugin>")
                .build();

        ScanDescriptorForHostClassesStage stage = new ScanDescriptorForHostClassesStage();
        SystemExports exports = new SystemExports("org.maera.plugin.osgi");
        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration>emptyList(), exports, new JarPluginArtifact(plugin),
                null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertFalse(context.getExtraImports().contains("blat"));
    }

    @Test
    public void testTransformWithHostComponentConstructorReferences() throws Exception {
        when(registration.getMainInterfaceClasses()).thenReturn(new Class<?>[]{SomeInterface.class});
        List<HostComponentRegistration> registrations = new ArrayList<HostComponentRegistration>(1);
        registrations.add(registration);

        final File plugin = new PluginJarBuilder("testUpgradeOfBundledPlugin")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='hostClass' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object key='hostClass' class='org.maera.plugin.osgi.HostClassUsingHostComponentConstructor'/>",
                        "</maera-plugin>")
                .build();

        ScanDescriptorForHostClassesStage stage = new ScanDescriptorForHostClassesStage();
        SystemExports exports = new SystemExports("org.maera.plugin.osgi");
        final TransformContext context = new TransformContext(registrations, exports, new JarPluginArtifact(plugin),
                null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertEquals(1, context.getRequiredHostComponents().size());
        assertEquals(registration, context.getRequiredHostComponents().iterator().next());
    }

    @Test
    public void testTransformWithHostComponentSetterReferences() throws Exception {
        when(registration.getMainInterfaceClasses()).thenReturn(new Class<?>[]{SomeInterface.class});
        List<HostComponentRegistration> registrations = new ArrayList<HostComponentRegistration>(1);
        registrations.add(registration);

        final File plugin = new PluginJarBuilder("testUpgradeOfBundledPlugin")
                .addFormattedResource("maera-plugin.xml",
                        "<maera-plugin name='Test' key='hostClass' pluginsVersion='2'>",
                        "    <plugin-info>",
                        "        <version>1.0</version>",
                        "    </plugin-info>",
                        "    <object key='hostClass' class='org.maera.plugin.osgi.HostClassUsingHostComponentSetter'/>",
                        "</maera-plugin>")
                .build();

        ScanDescriptorForHostClassesStage stage = new ScanDescriptorForHostClassesStage();
        SystemExports exports = new SystemExports("org.maera.plugin.osgi");
        final TransformContext context = new TransformContext(registrations, exports, new JarPluginArtifact(plugin),
                null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        stage.execute(context);
        assertEquals(1, context.getRequiredHostComponents().size());
        assertEquals(registration, context.getRequiredHostComponents().iterator().next());
    }
}