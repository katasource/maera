package org.maera.plugin.servlet.descriptors;

import junit.framework.TestCase;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.impl.StaticPlugin;

public class TestServletContextParamDescriptor extends TestCase {
    ServletContextParamModuleDescriptor descriptor;

    @Override
    public void setUp() {
        descriptor = new ServletContextParamModuleDescriptor();
    }

    @Override
    public void tearDown() {
        descriptor = null;
    }

    public void testInit() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        descriptor.init(plugin, e);
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
}
