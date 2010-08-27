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
import org.maera.plugin.module.PrefixDelegatingModuleFactory;
import org.maera.plugin.module.PrefixModuleFactory;
import org.maera.plugin.servlet.ServletModuleManager;
import org.maera.plugin.servlet.filter.FilterDispatcherCondition;
import org.maera.plugin.servlet.filter.FilterLocation;
import org.maera.plugin.servlet.filter.FilterTestUtils.FilterAdapter;

import java.util.Collections;

import static org.junit.Assert.*;

public class ServletFilterModuleDescriptorTest {

    ServletFilterModuleDescriptor descriptor;

    @Before
    public void setUp() {
        descriptor = new ServletFilterModuleDescriptor
                (new PrefixDelegatingModuleFactory(Collections.<PrefixModuleFactory>emptySet()), (ServletModuleManager) new Mock(ServletModuleManager.class).proxy());
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
        assertEquals(FilterLocation.BEFORE_DISPATCH, descriptor.getLocation());
        assertEquals(100, descriptor.getWeight());
    }

    @Test
    public void testInitWithBadDispatcher() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        Element badDispatcher = new DOMElement("dispatcher");
        badDispatcher.setText("badValue");
        e.add(badDispatcher);
        try {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        }
        catch (PluginParseException ex) {
            // very good
        }
    }

    @Test
    public void testInitWithBadLocation() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        e.addAttribute("location", "t23op");
        try {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        }
        catch (PluginParseException ex) {
            // very good
        }
    }

    @Test
    public void testInitWithBadWeight() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        e.addAttribute("weight", "t23op");
        try {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        }
        catch (PluginParseException ex) {
            // very good
        }
    }

    @Test
    public void testInitWithDetails() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = getValidConfig();
        e.addAttribute("location", "after-encoding");
        e.addAttribute("weight", "122");
        descriptor.init(plugin, e);
        assertEquals(FilterLocation.AFTER_ENCODING, descriptor.getLocation());
        assertEquals(122, descriptor.getWeight());
        assertTrue(descriptor.getDispatcherConditions().contains(FilterDispatcherCondition.REQUEST));
        assertTrue(descriptor.getDispatcherConditions().contains(FilterDispatcherCondition.FORWARD));
    }

    @Test
    public void testInitWithNoUrlPattern() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");
        Element e = new DOMElement("servlet-filter");
        e.addAttribute("key", "key2");
        e.addAttribute("class", FilterAdapter.class.getName());
        try {
            descriptor.init(plugin, e);
            fail("Should have thrown exception");
        }
        catch (PluginParseException ex) {
            // very good
        }
    }

    @Test
    public void testWithNoDispatcher() {
        Plugin plugin = new StaticPlugin();
        plugin.setKey("somekey");

        Element e = new DOMElement("servlet-filter");
        e.addAttribute("key", "key2");
        e.addAttribute("class", FilterAdapter.class.getName());
        Element url = new DOMElement("url-pattern");
        url.setText("/foo");
        e.add(url);

        descriptor.init(plugin, e);
    }

    private Element getValidConfig() {
        Element e = new DOMElement("servlet-filter");
        e.addAttribute("key", "key2");
        e.addAttribute("class", FilterAdapter.class.getName());
        Element url = new DOMElement("url-pattern");
        url.setText("/foo");
        e.add(url);
        Element dispatcher1 = new DOMElement("dispatcher");
        dispatcher1.setText("REQUEST");
        e.add(dispatcher1);
        Element dispatcher2 = new DOMElement("dispatcher");
        dispatcher2.setText("FORWARD");
        e.add(dispatcher2);
        return e;
    }
}
