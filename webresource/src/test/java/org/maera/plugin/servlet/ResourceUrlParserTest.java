package org.maera.plugin.servlet;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"deprecation"})
public class ResourceUrlParserTest {

    private ResourceUrlParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new ResourceUrlParser("resources");
    }

    @Test
    public void testMatches() {
        assertTrue(parser.matches("download/resources/test.plugin.key:module/test.css"));
        assertTrue(parser.matches("/download/resources/test.plugin.key:module/test.css"));
    }

    @Test
    public void testParseResourceWithSimpleName() {
        PluginResource resource = parser.parse("/download/resources/test.plugin.key:module/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("mydownload.jpg", resource.getResourceName());
    }

    @Test
    public void testParseResourceWithSlashesInName() {
        PluginResource resource = parser.parse("/download/resources/test.plugin.key:module/path/to/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("path/to/mydownload.jpg", resource.getResourceName());
    }
}
