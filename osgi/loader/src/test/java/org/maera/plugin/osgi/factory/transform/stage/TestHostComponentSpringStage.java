package org.maera.plugin.osgi.factory.transform.stage;

import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.osgi.SomeInterface;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.factory.transform.*;
import org.maera.plugin.osgi.factory.transform.model.SystemExports;
import org.maera.plugin.osgi.factory.transform.test.SomeClass;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.osgi.hostcomponents.impl.MockRegistration;
import org.maera.plugin.test.PluginJarBuilder;
import org.osgi.framework.ServiceReference;

import javax.servlet.Servlet;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestHostComponentSpringStage extends TestCase {

    private HostComponentSpringStage transformer = new HostComponentSpringStage();
    private File jar;
    private SystemExports systemExports;
    private OsgiContainerManager osgiContainerManager;

    @Override
    public void setUp() throws Exception {
        osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(org.maera.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Bundle-Version: 1.0",
                        "Bundle-SymbolicName: my.server",
                        "Bundle-ManifestVersion: 2",
                        "Bundle-ClassPath: .\n")
                .build();
        systemExports = new SystemExports("javax.servlet;version=\"2.3\",javax.servlet.http;version=\"2.3\"");
    }

    public void testTransformWithInnerJar() throws Exception, DocumentException {
        File outerjar = new PluginJarBuilder()
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Bundle-Version: 1.0",
                        "Bundle-SymbolicName: my.server",
                        "Bundle-ManifestVersion: 2",
                        "Bundle-ClassPath: .,foo.jar\n")
                .addFile("foo.jar", jar)
                .build();

        SpringTransformerTestHelper.transform(
                transformer,
                outerjar,
                new ArrayList<HostComponentRegistration>() {
                    {
                        add(new StubHostComponentRegistration("foo", new SomeInterface() {
                        }, SomeInterface.class));
                    }
                },
                null,
                "beans:bean[@id='foo']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))'");
    }

    public void testTransformWithInnerJarContainingInnerJar() throws Exception {
        // creates the jar file that is to be embedded in the inner jar
        final File embeddedJar = File.createTempFile("temp", ".jar", new File(System.getProperty("java.io.tmpdir")));
        final ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(embeddedJar));
        zout.putNextEntry(new ZipEntry("somefile"));
        zout.write("somefile".getBytes());
        zout.close();

        // create the inner jar embedding the jar file created in the previous step
        // this should be exactly the same as in the setUp() method, except for the temp.jar file entry.
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(org.maera.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .addFormattedResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0",
                        "Bundle-Version: 1.0",
                        "Bundle-SymbolicName: my.server",
                        "Bundle-ManifestVersion: 2",
                        "Bundle-ClassPath: .\n")
                .addFile("temp.jar", embeddedJar)
                .build();

        // delegates to the test method that tests transformation with inner jar, the assertions should be the same
        testTransformWithInnerJar();
    }

    public void testTransform() throws IOException, DocumentException {
        SpringTransformerTestHelper.transform(
                transformer,
                jar,
                new ArrayList<HostComponentRegistration>() {
                    {
                        add(new StubHostComponentRegistration("foo", new SomeInterface() {
                        }, SomeInterface.class));
                    }
                },
                null,
                "beans:bean[@id='foo']/beans:property[@name='interfaces']/beans:list/beans:value='" + SomeInterface.class.getName() + "'",
                "beans:bean[@id='foo']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))'");
    }

    public void testTransformWithProperNestedInferredImports() throws Exception {
        jar = new PluginJarBuilder().addFormattedJava("my.Foo", "package my;", "public class Foo {",
                "  public Foo(javax.swing.table.TableModel bar) {}", "}").addPluginInformation("my.plugin", "my.plugin", "1.0").build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {
            {
                add(new MockRegistration("foo", TableModel.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        transformer.execute(context);
        assertTrue(context.getExtraImports().contains("javax.swing.event"));

    }

    public void testTransformWithProperNestedVersionedInferredImports() throws Exception {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(javax.servlet.Servlet servlet) {}",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {
            {
                add(new MockRegistration("foo", Servlet.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        transformer.execute(context);
        assertTrue(context.getExtraImports().contains("javax.servlet;version=\"[2.3,2.3]\""));

    }

    public void testTransformWithInferredImportsOfSuperInterfaces() throws Exception {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(org.maera.plugin.osgi.factory.transform.FooChild bar) {}",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {
            {
                add(new MockRegistration("foo", FooChild.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        transformer.execute(context);
        assertTrue(context.getExtraImports().contains(SomeClass.class.getPackage().getName()));

    }

    public void testTransformWithSuperClassThatUsesHostComponent() throws Exception {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo extends " + AbstractFoo.class.getName() + " {",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {
            {
                add(new MockRegistration("foo", FooChild.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        transformer.execute(context);
        assertTrue(context.getExtraImports().contains(FooChild.class.getPackage().getName()));
    }

    public void testTransformWithSuperClassInJar() throws Exception {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "}")
                .addFormattedJava("my2.Bar",
                        "package my2;",
                        "public class Bar extends my.Foo {",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {
            {
                add(new MockRegistration("foo", FooChild.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        transformer.execute(context);
        assertEquals(0, context.getExtraImports().size());

    }

    public void testTransformWithSuperClassInOtherJar() throws Exception {
        PluginJarBuilder parent = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "}");

        jar = new PluginJarBuilder("child", parent.getClassLoader())
                .addFormattedJava("my2.Bar",
                        "package my2;",
                        "public class Bar extends my.Foo {",
                        "}")
                .addPluginInformation("my.plugin", "my.plugin", "1.0")
                .build();

        final List<HostComponentRegistration> regs = new ArrayList<HostComponentRegistration>() {
            {
                add(new MockRegistration("foo", FooChild.class));
            }
        };

        final TransformContext context = new TransformContext(regs, systemExports, new JarPluginArtifact(jar), null, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);
        transformer.execute(context);
        assertEquals(0, context.getExtraImports().size());

    }

    public void testTransformWithPoundSign() throws IOException, DocumentException {
        SpringTransformerTestHelper.transform(
                transformer,
                jar,
                new ArrayList<HostComponentRegistration>() {
                    {
                        add(new StubHostComponentRegistration("foo#1", new SomeInterface() {
                        }, SomeInterface.class));
                    }
                },
                null,
                "beans:bean[@id='fooLB1']/beans:property[@name='filter']/@value='(&(bean-name=foo#1)(plugins-host=true))'");
    }

    public void testTransformNoMatches() throws Exception {
        final File jar = new PluginJarBuilder().addFormattedJava("my.Foo", "package my;", "public class Foo {", "  public Foo(String bar) {}", "}").addPluginInformation(
                "my.plugin", "my.plugin", "1.0").addResource("META-INF/MANIFEST.MF",
                "Manifest-Version: 1.0\n" + "Bundle-Version: 1.0\n" + "Bundle-SymbolicName: my.server\n" + "Bundle-ManifestVersion: 2\n").build();

        // host component with name
        assertNull(SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>() {
            {
                add(new StubHostComponentRegistration("foo", SomeInterface.class));
            }
        }, null, "not(beans:bean[@id='foo'])"));
    }

    public void testTransformMatchInInnerJar() throws Exception {
        final File innerJar = new PluginJarBuilder().addFormattedJava("my.Foo", "package my;", "public class Foo {",
                "  public Foo(org.maera.plugin.osgi.SomeInterface bar) {}", "}").build();
        final File jar = new PluginJarBuilder().addFile("META-INF/lib/inner.jar", innerJar).addResource(
                "META-INF/MANIFEST.MF",
                "Manifest-Version: 1.0\n" + "Bundle-Version: 1.0\n" + "Bundle-SymbolicName: my.server\n" + "Bundle-ManifestVersion: 2\n" + "Bundle-ClassPath: .,\n" + "     META-INF/lib/inner.jar\n").addPluginInformation(
                "my.plugin", "my.plugin", "1.0").build();

        // host component with name
        SpringTransformerTestHelper.transform(transformer, jar, new ArrayList<HostComponentRegistration>() {
            {
                add(new StubHostComponentRegistration("foo", SomeInterface.class));
            }
        }, null, "beans:bean[@id='foo']");
    }

    public void testTransformWithExistingComponentImportName() throws Exception, DocumentException {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(org.maera.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin>",
                        "  <component-import key='foo' class='Foo' interface='Foo'/>",
                        "</atlassian-plugin>")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                                "Bundle-Version: 1.0\n" +
                                "Bundle-SymbolicName: my.server\n" +
                                "Bundle-ManifestVersion: 2\n")
                .build();

        SpringTransformerTestHelper.transform(
                transformer,
                jar,
                new ArrayList<HostComponentRegistration>() {
                    {
                        assertTrue(add(new StubHostComponentRegistration("foo", new SomeInterface() {
                        }, SomeInterface.class)));
                    }
                },
                null,
                "beans:bean[@id='foo0']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))'");
    }

    public void testTransformWithExistingComponentImportInterface() throws Exception, DocumentException {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(org.maera.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin>",
                        "  <component-import key='foobar'>",
                        "    <interface>org.maera.plugin.osgi.SomeInterface</interface>",
                        "  </component-import>",
                        "</atlassian-plugin>")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                                "Bundle-Version: 1.0\n" +
                                "Bundle-SymbolicName: my.server\n" +
                                "Bundle-ManifestVersion: 2\n")
                .build();

        SpringTransformerTestHelper.transform(
                transformer,
                jar,
                new ArrayList<HostComponentRegistration>() {
                    {
                        assertTrue(add(new StubHostComponentRegistration("foo", new SomeInterface() {
                        }, SomeInterface.class)));
                    }
                },
                null,
                "not(beans:bean[@id='foo']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))']");
    }

    public void testTransformWithExistingComponentImportInterfaceScopedToDifferentApplication() throws Exception, DocumentException {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(org.maera.plugin.osgi.SomeInterface bar) {}",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin>",
                        "  <component-import key='foobar' application='notfoo'>",
                        "    <interface>org.maera.plugin.osgi.SomeInterface</interface>",
                        "  </component-import>",
                        "</atlassian-plugin>")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                                "Bundle-Version: 1.0\n" +
                                "Bundle-SymbolicName: my.server\n" +
                                "Bundle-ManifestVersion: 2\n")
                .build();

        assertNotNull("No file overrides!", SpringTransformerTestHelper.transform(
                transformer,
                jar,
                new ArrayList<HostComponentRegistration>() {
                    {
                        assertTrue(add(new StubHostComponentRegistration("foo", new SomeInterface() {
                        }, SomeInterface.class)));
                    }
                },
                null,
                "beans:bean[@id='foo']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))'"));
    }

    public void testTransformWithExistingComponentImportInterfacePartialMatch() throws Exception, DocumentException {
        jar = new PluginJarBuilder()
                .addFormattedJava("my.Foo",
                        "package my;",
                        "public class Foo {",
                        "  public Foo(org.maera.plugin.osgi.factory.transform.Barable bar) {}",
                        "}")
                .addFormattedResource("atlassian-plugin.xml",
                        "<atlassian-plugin>",
                        "  <component-import key='foobar'>",
                        "    <interface>org.maera.plugin.osgi.factory.transform.Barable</interface>",
                        "  </component-import>",
                        "</atlassian-plugin>")
                .addResource("META-INF/MANIFEST.MF",
                        "Manifest-Version: 1.0\n" +
                                "Bundle-Version: 1.0\n" +
                                "Bundle-SymbolicName: my.server\n" +
                                "Bundle-ManifestVersion: 2\n")
                .build();

        SpringTransformerTestHelper.transform(
                transformer,
                jar,
                new ArrayList<HostComponentRegistration>() {
                    {
                        assertTrue(add(new StubHostComponentRegistration("foo", new Fooable() {
                            public SomeClass getSomeClass() {
                                return null;  //To change body of implemented methods use File | Settings | File Templates.
                            }
                        }, Barable.class, Fooable.class)));
                    }
                },
                null,
                "not(beans:bean[@id='foo']/beans:property[@name='filter']/@value='(&(bean-name=foo)(plugins-host=true))']");
    }

}
