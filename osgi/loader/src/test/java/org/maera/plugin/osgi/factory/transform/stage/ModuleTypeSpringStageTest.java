package org.maera.plugin.osgi.factory.transform.stage;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.osgi.external.SingleModuleDescriptorFactory;

import java.io.IOException;

import static org.junit.Assert.fail;

public class ModuleTypeSpringStageTest {

    @Test
    public void testTransform() throws IOException, DocumentException {
        Element pluginRoot = DocumentHelper.createDocument().addElement("maera-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");

        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "beans:bean[@id='springHostContainer' and @class='" + ModuleTypeSpringStage.SPRING_HOST_CONTAINER + "']",
                "beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "']",
                "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
    }

    @Test
    public void testTransformForOneApp() throws IOException, DocumentException {
        Element pluginRoot = DocumentHelper.createDocument().addElement("maera-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");
        moduleType.addAttribute("application", "bar");
        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "not(beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "'])");

        pluginRoot = DocumentHelper.createDocument().addElement("maera-plugin");
        moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");
        moduleType.addAttribute("class", "my.FooDescriptor");
        moduleType.addAttribute("application", "foo");
        SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                "beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "']");
    }

    @Test
    public void testTransformOfBadElement() throws IOException, DocumentException {
        Element pluginRoot = DocumentHelper.createDocument().addElement("maera-plugin");
        Element moduleType = pluginRoot.addElement("module-type");
        moduleType.addAttribute("key", "foo");

        try {
            SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                    "beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "']",
                    "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
            fail();
        } catch (PluginParseException ignored) {

        }
    }

    @Test
    public void testTransformOfBadElementKey() throws IOException, DocumentException {
        Element pluginRoot = DocumentHelper.createDocument().addElement("maera-plugin");
        pluginRoot.addElement("module-type");

        try {
            SpringTransformerTestHelper.transform(new ModuleTypeSpringStage(), pluginRoot,
                    "beans:bean[@id='moduleType-foo' and @class='" + SingleModuleDescriptorFactory.class.getName() + "']",
                    "osgi:service[@id='moduleType-foo_osgiService' and @auto-export='interfaces']");
            fail();
        } catch (PluginParseException ignored) {

        }
    }
}
