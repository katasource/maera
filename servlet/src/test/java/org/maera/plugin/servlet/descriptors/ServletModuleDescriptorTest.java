package org.maera.plugin.servlet.descriptors;

import com.mockobjects.dynamic.Mock;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.impl.StaticPlugin;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.servlet.ServletModuleManager;

import javax.servlet.http.HttpServlet;

import static org.junit.Assert.fail;

public class ServletModuleDescriptorTest {

    private ServletModuleDescriptor descriptor;

    @Before
    public void setUp() {
        descriptor = new ServletModuleDescriptor(ModuleFactory.LEGACY_MODULE_FACTORY, (ServletModuleManager) new Mock(ServletModuleManager.class).proxy());
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
    public void testInitWithMissingParamValue() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet");
        e.addAttribute("key", "key2");
        e.addAttribute("class", SomeServlet.class.getName());
        Element url = new DOMElement("url-pattern");
        url.setText("/foo");
        e.add(url);
        Element param = new DOMElement("init-param");
        e.add(param);
        try {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        } catch (PluginParseException ex) {
            // very good
        }
    }

    @Test
    public void testInitWithNoUrlPattern() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet");
        e.addAttribute("key", "key2");
        e.addAttribute("class", SomeServlet.class.getName());
        try {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        }
        catch (PluginParseException ex) {
            // very good
        }
    }

    private Element getValidConfig() {
        Element e = new DOMElement("servlet");
        e.addAttribute("key", "key2");
        e.addAttribute("class", SomeServlet.class.getName());
        Element url = new DOMElement("url-pattern");
        url.setText("/foo");
        e.add(url);
        return e;
    }

    static class SomeServlet extends HttpServlet {
    }
}
