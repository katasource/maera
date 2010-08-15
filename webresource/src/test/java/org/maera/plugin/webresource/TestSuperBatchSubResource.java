package org.maera.plugin.webresource;

import junit.framework.TestCase;

import java.util.Collections;

public class TestSuperBatchSubResource extends TestCase {
    public void testParsePluginResource() {
        String path = "/download/superbatch/css/images/foo.png";
        assertTrue(SuperBatchSubResource.matches(path));
        SuperBatchSubResource resource = SuperBatchSubResource.parse(path, Collections.<String, String>emptyMap());
        assertEquals("png", resource.getType());
        assertEquals("css/images/foo.png", resource.getResourceName());
    }
}
