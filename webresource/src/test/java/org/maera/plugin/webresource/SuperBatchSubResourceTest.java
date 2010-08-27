package org.maera.plugin.webresource;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SuperBatchSubResourceTest {

    @Test
    public void testParsePluginResource() {
        String path = "/download/superbatch/css/images/foo.png";
        assertTrue(SuperBatchSubResource.matches(path));
        SuperBatchSubResource resource = SuperBatchSubResource.parse(path, Collections.<String, String>emptyMap());
        assertEquals("png", resource.getType());
        assertEquals("css/images/foo.png", resource.getResourceName());
    }
}
