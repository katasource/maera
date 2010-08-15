package org.maera.plugin.osgi.factory.transform.stage;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.model.SystemExports;
import org.maera.plugin.test.PluginJarBuilder;
import org.maera.plugin.util.validation.ValidationException;
import org.osgi.framework.ServiceReference;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestComponentSpringStage extends TestCase {
    public void testTransform() throws IOException, DocumentException {
        ComponentSpringStage transformer = new ComponentSpringStage();

        // private component
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']");

        // public component, interface
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                "osgi:service[@id='foo_osgiService' and @ref='foo']",
                "//osgi:interfaces",
                "//beans:value[.='my.IFoo']");

        // public component, interface as attribute
        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        component.addAttribute("interface", "my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                "osgi:service[@id='foo_osgiService' and @ref='foo']",
                "//osgi:interfaces",
                "//beans:value[.='my.IFoo']");

    }

    public void testTransformWithServiceProperties() throws IOException, DocumentException {
        ComponentSpringStage transformer = new ComponentSpringStage();

        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        component.addAttribute("interface", "my.IFoo");

        Element svcprops = component.addElement("service-properties");
        Element prop = svcprops.addElement("entry");
        prop.addAttribute("key", "foo");
        prop.addAttribute("value", "bar");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                "osgi:service[@id='foo_osgiService']/osgi:service-properties",
                "osgi:service[@id='foo_osgiService']/osgi:service-properties/beans:entry[@key='foo' and @value='bar']",
                "//osgi:interfaces",
                "//beans:value[.='my.IFoo']");

        svcprops.clearContent();
        try {
            SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']",
                    "osgi:service[@id='foo_osgiService']/osgi:service-properties",
                    "//osgi:interfaces",
                    "//beans:value[.='my.IFoo']");
            fail("Validation exception should have been thrown");
        }
        catch (ValidationException ex) {
            // expected
        }
    }

    public void testTransformForOneApp() throws IOException, DocumentException {
        ComponentSpringStage transformer = new ComponentSpringStage();

        // public component, interface
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        component.addAttribute("application", "bar");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "not(beans:bean[@id='foo' and @class='my.Foo'])");

        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        component.addAttribute("application", "foo");
        inf = component.addElement("interface");
        inf.setText("my.IFoo");
        SpringTransformerTestHelper.transform(transformer, pluginRoot, "beans:bean[@id='foo' and @class='my.Foo']");

    }

    public void testExportsAdded() throws IOException {
        ComponentSpringStage transformer = new ComponentSpringStage();
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");

        Mock mockPluginArtifact = new Mock(PluginArtifact.class);
        mockPluginArtifact.matchAndReturn("toFile", new PluginJarBuilder().build());
        mockPluginArtifact.expectAndReturn("getResourceAsStream", C.args(C.eq("foo")),
                new ByteArrayInputStream(SpringTransformerTestHelper.elementToString(pluginRoot).getBytes()));
        mockPluginArtifact.expectAndReturn("doesResourceExist", C.args(C.eq("my/IFoo.class")), true);
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        TransformContext ctx = new TransformContext(null, SystemExports.NONE, (PluginArtifact) mockPluginArtifact.proxy(), null, "foo", osgiContainerManager);
        transformer.execute(ctx);

        assertTrue(ctx.getExtraExports().contains("my"));
        mockPluginArtifact.verify();
    }

    public void testExportsNotInJar() throws IOException {
        ComponentSpringStage transformer = new ComponentSpringStage();
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");

        Mock mockPluginArtifact = new Mock(PluginArtifact.class);
        mockPluginArtifact.matchAndReturn("toFile", new PluginJarBuilder().build());
        mockPluginArtifact.expectAndReturn("getResourceAsStream", C.args(C.eq("foo")),
                new ByteArrayInputStream(SpringTransformerTestHelper.elementToString(pluginRoot).getBytes()));
        mockPluginArtifact.expectAndReturn("doesResourceExist", C.args(C.eq("my/IFoo.class")), false);
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        TransformContext ctx = new TransformContext(null, SystemExports.NONE, (PluginArtifact) mockPluginArtifact.proxy(), null, "foo", osgiContainerManager);
        transformer.execute(ctx);

        assertFalse(ctx.getExtraExports().contains("my"));
        mockPluginArtifact.verify();
    }

    public void testExportsExist() throws IOException {
        ComponentSpringStage transformer = new ComponentSpringStage();
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element component = pluginRoot.addElement("component");
        component.addAttribute("key", "foo");
        component.addAttribute("class", "my.Foo");
        component.addAttribute("public", "true");
        Element inf = component.addElement("interface");
        inf.setText("my.IFoo");

        Mock mockPluginArtifact = new Mock(PluginArtifact.class);
        mockPluginArtifact.matchAndReturn("toFile", new PluginJarBuilder().build());
        mockPluginArtifact.expectAndReturn("getResourceAsStream", C.args(C.eq("foo")),
                new ByteArrayInputStream(SpringTransformerTestHelper.elementToString(pluginRoot).getBytes()));
        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);
        TransformContext ctx = new TransformContext(null, SystemExports.NONE, (PluginArtifact) mockPluginArtifact.proxy(), null, "foo", osgiContainerManager);
        ctx.getExtraExports().add("my");
        transformer.execute(ctx);

        assertEquals(ctx.getExtraExports().indexOf("my"), ctx.getExtraExports().lastIndexOf("my"));
        mockPluginArtifact.verify();
    }
}
