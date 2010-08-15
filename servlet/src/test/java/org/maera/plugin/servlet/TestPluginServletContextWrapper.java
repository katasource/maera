package org.maera.plugin.servlet;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.Plugin;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TestPluginServletContextWrapper extends TestCase {
    Mock mockServletContext;
    Mock mockPlugin;

    ConcurrentMap<String, Object> attributes;
    Map<String, String> initParams;

    ServletContext contextWrapper;

    public void setUp() {
        mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getAttribute", C.eq("wrapped"), "wrapped value");

        mockPlugin = new Mock(Plugin.class);

        attributes = new ConcurrentHashMap<String, Object>();
        initParams = new HashMap<String, String>();

        contextWrapper = new PluginServletContextWrapper((Plugin) mockPlugin.proxy(), (ServletContext) mockServletContext.proxy(), attributes, initParams);
    }

    public void testPutAttribute() {
        // if set attribute is called on the wrapped context it will throw an 
        // exception since it is not expecting it
        contextWrapper.setAttribute("attr", "value");
        assertEquals("value", contextWrapper.getAttribute("attr"));
    }

    public void testGetAttributeDelegatesToWrappedContext() {
        assertEquals("wrapped value", contextWrapper.getAttribute("wrapped"));
    }

    public void testPutAttributeOverridesWrapperContextAttribute() {
        // if set attribute is called on the wrapped context it will throw an 
        // exception since it is not expecting it
        contextWrapper.setAttribute("wrapped", "value");
        assertEquals("value", contextWrapper.getAttribute("wrapped"));
    }
}
