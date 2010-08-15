package org.maera.plugin.osgi.factory.transform.stage;

import junit.framework.TestCase;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.osgi.external.SingleModuleDescriptorFactory;

import java.io.IOException;

public class TestModuleTypeSpringStage extends TestCase {
    public void testTransform() throws IOException, DocumentException {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");

        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "beans:bean[@id='springHostContainer' and @class='" + ModuleTypeSpringStage.SPRING_HOST_CONTAINER + "']",
                "beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "']",
                "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
    }

    public void testTransformForOneApp() throws IOException, DocumentException {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");
        moduleType.addAttribute("application", "bar");
        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "not(beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "'])");

        pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");
        moduleType.addAttribute("application", "foo");
        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "']");
    }

    public void testTransformOfBadElement() throws IOException, DocumentException {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");

        try {
            SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                    "beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "']",
                    "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
            fail();
        }
        catch (PluginParseException ex) {
            // pass
        }
    }

    public void testTransformOfBadElementKey() throws IOException, DocumentException {
        Element pluginRoot = DocumentHelper.createDocument().addElement("atlassian-plugin");
        Element moduleType = pluginRoot.addElement("module-type");

        try {
            SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                    "beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "']",
                    "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
            fail();
        }
        catch (PluginParseException ex) {
            // pass
        }
    }

}
