package org.maera.plugin.servlet;

import junit.framework.TestCase;

public class TestResourceUrlParser extends TestCase {
    private ResourceUrlParser parser;

    protected void setUp() throws Exception {
        parser = new ResourceUrlParser("resources");
    }

    public void testMatches() {
        assertTrue(parser.matches("download/resources/test.plugin.key:module/test.css"));
        assertTrue(parser.matches("/download/resources/test.plugin.key:module/test.css"));
    }

    public void testParseResourceWithSimpleName() {
        PluginResource resource = parser.parse("/download/resources/test.plugin.key:module/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("mydownload.jpg", resource.getResourceName());
    }

    public void testParseResourceWithSlashesInName() {
        PluginResource resource = parser.parse("/download/resources/test.plugin.key:module/path/to/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("path/to/mydownload.jpg", resource.getResourceName());
    }
}
