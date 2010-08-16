package org.maera.plugin.webresource;

import com.mockobjects.dynamic.Mock;
import org.dom4j.DocumentHelper;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.Plugin;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class WebResourceModuleDescriptorTest {

    private static final String TEST_PLUGIN_KEY = "maera.test.plugin";

    private WebResourceModuleDescriptor descriptor;
    private Mock mockPlugin;

    @Before
    public void setUp() throws Exception {
        descriptor = new WebResourceModuleDescriptor();
        mockPlugin = new Mock(Plugin.class);
        mockPlugin.matchAndReturn("getKey", TEST_PLUGIN_KEY);
    }

    @Test
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
