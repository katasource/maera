package org.maera.plugin.webresource;

import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.dom4j.DocumentHelper;
import org.maera.plugin.Plugin;

import java.util.List;

public class TestWebResourceModuleDescriptor extends TestCase {
    private static final String TEST_PLUGIN_KEY = "maera.test.plugin";

    private WebResourceModuleDescriptor descriptor;
    private Mock mockPlugin;

    protected void setUp() throws Exception {
        super.setUp();
        descriptor = new WebResourceModuleDescriptor();
        mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getKey", TEST_PLUGIN_KEY);
    }

    protected void tearDown() throws Exception {
        descriptor = null;
        mockPlugin = null;

        super.tearDown();
    }

    public void testInitWithDependencies() throws Exception {
        String xml = "<web-resource key=\"test-resources\">\n" +
                "<dependency>maera.test.plugin:jquery</dependency>\n" +
                "<dependency>maera.test.plugin:ajs</dependency>\n" +
                "</web-resource>";

        descriptor.init((Plugin) mockPlugin.proxy(), DocumentHelper.parseText(xml).getRootElement());

        List<String> dependencies = descriptor.getDependencies();
        assertEquals(2, dependencies.size());
        assertEquals("maera.test.plugin:jquery", dependencies.get(0));
        assertEquals("maera.test.plugin:ajs", dependencies.get(1));
    }
}
