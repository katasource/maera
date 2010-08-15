package org.maera.plugin.webresource;

import junit.framework.TestCase;

public class TestPluginResource extends TestCase {
    public void testParseWithSimpleName() throws Exception {
        SinglePluginResource resource = SinglePluginResource.parse("/download/resources/test.plugin.key:module/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("mydownload.jpg", resource.getResourceName());
    }

    public void testParseWithSlashesInName() throws Exception {
        SinglePluginResource resource = SinglePluginResource.parse("/download/resources/test.plugin.key:module/path/to/mydownload.jpg");
        assertEquals("test.plugin.key:module", resource.getModuleCompleteKey());
        assertEquals("path/to/mydownload.jpg", resource.getResourceName());
    }

    public void testGetUrl() throws Exception {
        SinglePluginResource resource = new SinglePluginResource("foo.css", "test.plugin.key", false);
        assertEquals("/download/resources/test.plugin.key/foo.css", resource.getUrl());
    }

    public void testRoundTrip() throws Exception {
        SinglePluginResource resource = new SinglePluginResource("foo.css", "test.plugin.key", false);
        String url = resource.getUrl();
        SinglePluginResource parsedResource = SinglePluginResource.parse(url);
        assertEquals(resource.getModuleCompleteKey(), parsedResource.getModuleCompleteKey());
        assertEquals(resource.getResourceName(), parsedResource.getResourceName());
    }

    public void testParseInvlaidUrlThrowsException() {
        try {
            SinglePluginResource.parse("/download/resources/blah.png");
            fail("Should have thrown exception for invalid url");
        }
        catch (UrlParseException e) {
            //expected
        }
    }
}
