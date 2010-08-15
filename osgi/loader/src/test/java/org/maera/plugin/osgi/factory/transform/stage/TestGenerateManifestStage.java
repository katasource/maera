package org.maera.plugin.osgi.factory.transform.stage;

import junit.framework.TestCase;
import org.apache.log4j.spi.Filter;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.factory.OsgiPlugin;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.model.SystemExports;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.test.PluginJarBuilder;
import org.maera.plugin.util.PluginUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import javax.print.attribute.AttributeSet;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.*;

public class TestGenerateManifestStage extends TestCase {
    private GenerateManifestStage stage;
    private OsgiContainerManager osgiContainerManager;

    @Override
    public void setUp() {
        stage = new GenerateManifestStage();
        osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
    }


    public void testGenerateManifest() throws Exception {
        final File file = new PluginJarBuilder()
                .addFormattedResource(
                        "atlassian-plugin.xml",
                        "<atlassian-plugin key='org.maera.plugins.example' name='Example Plugin'>",
                        "  <plugin-info>",
                        "    <description>",
                        "      A sample plugin for demonstrating the file format.",
                        "    </description>",
                        "    <version>1.1</version>",
                        "    <vendor name='Atlassian Software Systems Pty Ltd' url='http://www.atlassian.com'/>",
                        "  </plugin-info>",
                        "</atlassian-plugin>")
                .addFormattedJava(
                        "com.mycompany.myapp.Foo",
                        "package com.mycompany.myapp; public class Foo {}")
                .build();

        final TransformContext context = new TransformContext(Collections.<HostComponentRegistration>emptyList(), SystemExports.NONE, new JarPluginArtifact(file),
                null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        context.setShouldRequireSpring(true);

        final Attributes attrs = executeStage(context);

        assertEquals("1.1", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("org.maera.plugins.example", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("A sample plugin for demonstrating the file format.", attrs.getValue(Constants.BUNDLE_DESCRIPTION));
        assertEquals("Atlassian Software Systems Pty Ltd", attrs.getValue(Constants.BUNDLE_VENDOR));
        assertEquals("http://www.atlassian.com", attrs.getValue(Constants.BUNDLE_DOCURL));
        assertEquals(null, attrs.getValue(Constants.EXPORT_PACKAGE));
        assertEquals(".", attrs.getValue(Constants.BUNDLE_CLASSPATH));
        assertEquals("org.maera.plugins.example", attrs.getValue(OsgiPlugin.ATLASSIAN_PLUGIN_KEY));
        assertEquals("*;timeout:=60", attrs.getValue("Spring-Context"));
        assertEquals(null, attrs.getValue(Constants.IMPORT_PACKAGE));

    }

    public void testGenerateManifestWithProperInferredImports() throws Exception {

        final File file = new PluginJarBuilder().addPluginInformation("someKey", "someName", "1.0").build();
        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(file), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        context.getExtraImports().add(AttributeSet.class.getPackage().getName());
        final Attributes attrs = executeStage(context);

        assertTrue(attrs.getValue(Constants.IMPORT_PACKAGE).contains(AttributeSet.class.getPackage().getName()));

    }

    public void testGenerateManifestWithCustomTimeout() throws Exception {
        try {
            System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "333");
            stage = new GenerateManifestStage();
            final File file = new PluginJarBuilder().addPluginInformation("someKey", "someName", "1.0").build();
            final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(file), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
            context.setShouldRequireSpring(true);
            final Attributes attrs = executeStage(context);

            assertEquals("*;timeout:=333", attrs.getValue("Spring-Context"));
        }
        finally {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }

    }

    public void testGenerateManifestWithExistingSpringContextTimeout() throws Exception {
        try {
            System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "333");
            stage = new GenerateManifestStage();
            final File file = new PluginJarBuilder().addPluginInformation("someKey", "someName", "1.0")
                    .addFormattedResource("META-INF/MANIFEST.MF",
                            "Manifest-Version: 1.0",
                            "Spring-Context: *;timeout:=60",
                            "Bundle-Version: 4.2.0.jira40",
                            "Bundle-SymbolicName: my.foo.symbolicName")
                    .build();
            final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(file), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
            context.setShouldRequireSpring(true);
            final Attributes attrs = executeStage(context);

            assertEquals("*;timeout:=333", attrs.getValue("Spring-Context"));
        }
        finally {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    public void testGenerateManifestWithExistingSpringContextTimeoutNoSystemProperty() throws Exception {
        stage = new GenerateManifestStage();
        final File file = new PluginJarBuilder().addPluginInformation("someKey", "someName", "1.0")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Spring-Context: *;timeout:=60",
                        "Bundle-Version: 4.2.0.jira40",
                        "Bundle-SymbolicName: my.foo.symbolicName")
                .build();
        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(file), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        context.setShouldRequireSpring(true);
        final Attributes attrs = executeStage(context);

        assertEquals("*;timeout:=60", attrs.getValue("Spring-Context"));
    }

    public void testGenerateManifestSpringContextTimeoutNoTimeoutInHeader() throws Exception {
        try {
            System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "789");
            stage = new GenerateManifestStage();
            final File file = new PluginJarBuilder().addPluginInformation("someKey", "someName", "1.0")
                    .addFormattedResource("META-INF/MANIFEST.MF",
                            "Manifest-Version: 1.0",
                            "Spring-Context: *;create-asynchronously:=false",
                            "Bundle-Version: 4.2.0.jira40",
                            "Bundle-SymbolicName: my.foo.symbolicName")
                    .build();
            final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(file), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
            context.setShouldRequireSpring(true);
            final Attributes attrs = executeStage(context);
            assertEquals("*;create-asynchronously:=false;timeout:=789", attrs.getValue("Spring-Context"));
        }
        finally {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    public void testGenerateManifestSpringContextTimeoutTimeoutAtTheBeginning() throws Exception {
        try {
            System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "789");
            stage = new GenerateManifestStage();
            final File file = new PluginJarBuilder().addPluginInformation("someKey", "someName", "1.0")
                    .addFormattedResource("META-INF/MANIFEST.MF",
                            "Manifest-Version: 1.0",
                            "Spring-Context: timeout:=123;config/account-data-context.xml;create-asynchrously:=false",
                            "Bundle-Version: 4.2.0.jira40",
                            "Bundle-SymbolicName: my.foo.symbolicName")
                    .build();
            final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(file), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
            context.setShouldRequireSpring(true);
            final Attributes attrs = executeStage(context);
            assertEquals("timeout:=789;config/account-data-context.xml;create-asynchrously:=false", attrs.getValue("Spring-Context"));
        }
        finally {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }

    public void testGenerateManifestSpringContextTimeoutTimeoutInTheMiddle() throws Exception {
        try {
            System.setProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT, "789");
            stage = new GenerateManifestStage();
            final File file = new PluginJarBuilder().addPluginInformation("someKey", "someName", "1.0")
                    .addFormattedResource("META-INF/MANIFEST.MF",
                            "Manifest-Version: 1.0",
                            "Spring-Context: config/account-data-context.xml;timeout:=123;create-asynchrously:=false",
                            "Bundle-Version: 4.2.0.jira40",
                            "Bundle-SymbolicName: my.foo.symbolicName")
                    .build();
            final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(file), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
            context.setShouldRequireSpring(true);
            final Attributes attrs = executeStage(context);
            assertEquals("config/account-data-context.xml;timeout:=789;create-asynchrously:=false", attrs.getValue("Spring-Context"));
        }
        finally {
            System.clearProperty(PluginUtils.ATLASSIAN_PLUGINS_ENABLE_WAIT);
        }
    }


    public void testGenerateManifestMergeHostComponentImportsWithExisting() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Import-Package: javax.swing",
                        "Bundle-SymbolicName: my.foo.symbolicName",
                        "Bundle-ClassPath: .,foo")
                .addResource("foo/bar.txt", "Something")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        context.getExtraImports().add(AttributeSet.class.getPackage().getName());
        final Attributes attrs = executeStage(context);
        assertEquals("my.foo.symbolicName", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals(".,foo", attrs.getValue(Constants.BUNDLE_CLASSPATH));
        assertEquals("innerjarcp", attrs.getValue(OsgiPlugin.ATLASSIAN_PLUGIN_KEY));
        final String importPackage = attrs.getValue(Constants.IMPORT_PACKAGE);
        assertTrue(importPackage.contains(AttributeSet.class.getPackage().getName() + ","));
        assertTrue(importPackage.contains("javax.swing"));

    }

    public void testGenerateManifestInvalidVersionWithExisting() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Bundle-SymbolicName: my.foo.symbolicName",
                        "Bundle-Version: beta1",
                        "Bundle-ClassPath: .,foo\n")
                .addResource("foo/bar.txt", "Something")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        try {
            executeStage(context);
            fail("Should have complained about bad plugin version");
        }
        catch (PluginParseException ex) {
            ex.printStackTrace();
            // expected
        }
    }

    public void testGenerateManifestInvalidVersion() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addPluginInformation("innerjarcp", "Some name", "beta1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        try {
            executeStage(context);
            fail("Should have complained about bad plugin version");
        }
        catch (PluginParseException ex) {
            ex.printStackTrace();
            // expected
        }

    }

    public void testGenerateManifestWithExistingManifestNoSpringButDescriptor() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Bundle-SymbolicName: my.foo.symbolicName",
                        "Bundle-Version: 1.0",
                        "Bundle-ClassPath: .,foo")
                .addResource("foo/bar.txt", "Something")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        final Attributes attrs = executeStage(context);
        assertEquals("innerjarcp", attrs.getValue("Atlassian-Plugin-Key"));
        assertNotNull(attrs.getValue("Spring-Context"));

    }

    public void testGenerateManifestWithExistingManifestWithSpringWithDescriptor() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Bundle-SymbolicName: my.foo.symbolicName",
                        "Bundle-Version: 1.0",
                        "Spring-Context: *",
                        "Bundle-ClassPath: .,foo")
                .addResource("foo/bar.txt", "Something")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        final Attributes attrs = executeStage(context);
        assertEquals("innerjarcp", attrs.getValue("Atlassian-Plugin-Key"));
        assertEquals("*", attrs.getValue("Spring-Context"));

    }

    public void testGenerateManifestNoExistingManifestButDescriptor() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addResource("foo/bar.txt", "Something")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        final Attributes attrs = executeStage(context);
        assertEquals("innerjarcp", attrs.getValue("Atlassian-Plugin-Key"));
        assertNotNull(attrs.getValue("Spring-Context"));

    }

    public void testGenerateManifestWarnNoTimeout() throws Exception {
        org.slf4j.Logger log = mock(org.slf4j.Logger.class);
        GenerateManifestStage.log = log;

        final File plugin = new PluginJarBuilder("plugin")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Import-Package: javax.swing",
                        "Bundle-SymbolicName: my.foo.symbolicName",
                        "Spring-Context: *",
                        "Bundle-ClassPath: .,foo")
                .addResource("foo/bar.txt", "Something")
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        context.setShouldRequireSpring(true);
        context.getExtraImports().add(AttributeSet.class.getPackage().getName());
        executeStage(context);
        verify(log).warn(contains("Please add ';timeout:=60'"));
    }


    public void testGenerateManifest_innerjars() throws URISyntaxException, PluginParseException, IOException {
        final File innerJar = new PluginJarBuilder("innerjar1").build();
        final File innerJar2 = new PluginJarBuilder("innerjar2").build();
        final File plugin = new PluginJarBuilder("plugin")
                .addFile("META-INF/lib/innerjar.jar", innerJar)
                .addFile("META-INF/lib/innerjar2.jar", innerJar2)
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        final Attributes attrs = executeStage(context);

        final Collection classpathEntries = Arrays.asList(attrs.getValue(Constants.BUNDLE_CLASSPATH).split(","));
        assertEquals(3, classpathEntries.size());
        assertTrue(classpathEntries.contains("."));
        assertTrue(classpathEntries.contains("META-INF/lib/innerjar.jar"));
        assertTrue(classpathEntries.contains("META-INF/lib/innerjar2.jar"));
    }

    public void testGenerateManifest_innerjarsInImports() throws Exception, PluginParseException, IOException {
        final File innerJar = new PluginJarBuilder("innerjar")
                .addFormattedJava("my.Foo",
                        "package my;",
                        "import org.apache.log4j.Logger;",
                        "public class Foo{",
                        "   Logger log;",
                        "}")
                .build();
        assertNotNull(innerJar);
        final File plugin = new PluginJarBuilder("plugin")
                .addJava("my.Bar", "package my;import org.apache.log4j.spi.Filter; public class Bar{Filter log;}")
                .addFile("META-INF/lib/innerjar.jar", innerJar)
                .addPluginInformation("innerjarcp", "Some name", "1.0")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        final Attributes attrs = executeStage(context);

        assertEquals("1.0", attrs.getValue(Constants.BUNDLE_VERSION));
        assertEquals("innerjarcp", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));

        final Collection classpathEntries = Arrays.asList(attrs.getValue(Constants.BUNDLE_CLASSPATH).split(","));
        assertEquals(2, classpathEntries.size());
        assertTrue(classpathEntries.contains("."));
        assertTrue(classpathEntries.contains("META-INF/lib/innerjar.jar"));

        final Collection imports = Arrays.asList(attrs.getValue("Import-Package").split(","));
        assertEquals(2, imports.size());
        assertTrue(imports.contains(org.apache.log4j.Logger.class.getPackage().getName() + ";resolution:=\"optional\""));
        assertTrue(imports.contains(Filter.class.getPackage().getName() + ";resolution:=\"optional\""));
    }

    public void testGenerateManifestWithBundleInstructions() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addPluginInformation("test.plugin", "test.plugin", "1.0")
                .addJava("foo.MyClass", "package foo; public class MyClass{}")
                .addJava("foo.internal.MyPrivateClass", "package foo.internal; public class MyPrivateClass{}")
                .build();

        final TransformContext context = new TransformContext(null, SystemExports.NONE, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        context.getBndInstructions().put("Export-Package", "!*.internal.*,*");
        final Attributes attrs = executeStage(context);
        assertEquals("test.plugin", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));
        assertEquals("foo", attrs.getValue(Constants.EXPORT_PACKAGE));
    }

    public void testGenerateManifestWithHostAndExternalImports() throws Exception {
        final File plugin = new PluginJarBuilder("plugin")
                .addPluginInformation("test.plugin", "test.plugin", "1.0")
                .build();

        SystemExports exports = new SystemExports("foo.bar,foo.baz;version=\"1.0\"");
        final TransformContext context = new TransformContext(null, exports, new JarPluginArtifact(plugin), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        context.getBndInstructions().put("Import-Package", "foo.bar,foo.baz");
        final Attributes attrs = executeStage(context);
        assertEquals("test.plugin", attrs.getValue(Constants.BUNDLE_SYMBOLICNAME));

        String imports = attrs.getValue(Constants.IMPORT_PACKAGE);
        assertTrue(imports.contains("foo.baz;version=\"[1.0,1.0]\""));
        assertTrue(imports.contains("foo.bar"));
    }

    private Attributes executeStage(final TransformContext context) throws IOException {
        stage.execute(context);
        final Manifest mf = new Manifest(new ByteArrayInputStream(context.getFileOverrides().get("META-INF/MANIFEST.MF")));
        final Attributes attrs = mf.getMainAttributes();
        return attrs;
    }
}
