package org.maera.plugin.servlet.descriptors;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.impl.StaticPlugin;

import static org.junit.Assert.fail;

public class ServletContextParamDescriptorTest {

    ServletContextParamModuleDescriptor descriptor;

    @Before
    public void setUp() {
        descriptor = new ServletContextParamModuleDescriptor();
    }

    @After
    public void tearDown() {
        descriptor = null;
    }

    @Test
    public void testInit() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        descriptor.init(plugin, e);
    }

    @Test
    public void testInitWithNoParamName() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet-context-param");
        e.addAttribute("key", "key2");
        Element paramValue = new DOMElement("param-value");
        paramValue.setText("test.param.value");
        e.add(paramValue);
        try {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        } catch (PluginParseException ex) {
            // very good
        }
    }

    @Test
    public void testInitWithNoParamValue() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet-context-param");
        e.addAttribute("key", "key2");
        Element paramName = new DOMElement("param-name");
        paramName.setText("test.param.name");
        e.add(paramName);
        try {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        } catch (PluginParseException ex) {
            // very good
        }
    }

    private Element getValidConfig() {
        Element e = new DOMElement("servlet-context-param");
        e.addAttribute("key", "key2");
        Element paramName = new DOMElement("param-name");
        paramName.setText("test.param.name");
        e.add(paramName);
        Element paramValue = new DOMElement("param-value");
        paramValue.setText("test.param.value");
        e.add(paramValue);
        return e;
    }
}
