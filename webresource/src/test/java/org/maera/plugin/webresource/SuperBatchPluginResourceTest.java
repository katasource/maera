package org.maera.plugin.webresource;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class SuperBatchPluginResourceTest {

    @Test
    public void testGetType() {
        assertEquals("css", SuperBatchPluginResource.getType("/foo.css"));
        assertEquals("js", SuperBatchPluginResource.getType("/superbatch/js/foo.js"));
        assertEquals("", SuperBatchPluginResource.getType("/superbatch/js/foo."));
        assertEquals("", SuperBatchPluginResource.getType("/superbatch/js/foo"));
    }

    @Test
    public void testNotSuperbatches() {
        assertFalse("wrong path", SuperBatchPluginResource.matches("/download/superbitch/css/batch.css"));
        assertFalse("wrong path", SuperBatchPluginResource.matches("/download/superbatch/css/images/foo.png"));
    }

    @Test
    public void testParseCss() {
        String path = "/download/superbatch/css/batch.css";
        assertTrue(SuperBatchPluginResource.matches(path));
        SuperBatchPluginResource resource = SuperBatchPluginResource.parse(path, Collections.<String, String>emptyMap());
        assertEquals("css", resource.getType());
        assertEquals(resource.getUrl(), path);
        assertEquals("batch.css", resource.getResourceName());
    }

    @Test
    public void testParseJavascript() {
        String path = "/download/superbatch/js/batch.js";
        assertTrue(SuperBatchPluginResource.matches(path));
        SuperBatchPluginResource resource = SuperBatchPluginResource.parse(path, Collections.<String, String>emptyMap());
        assertEquals("js", resource.getType());
        assertEquals(resource.getUrl(), path);
        assertEquals("batch.js", resource.getResourceName());
    }

    // For some reason the download manager doesn't strip context paths before sending it in to be matched.
    @Test
    public void testParseWithContextPath() {
        assertTrue(SuperBatchPluginResource.matches("/confluence/download/superbatch/css/batch.css"));
    }

    @Test
    public void testParseWithParam() {
        String path = "/download/superbatch/js/batch.js";
        Map<String, String> params = Collections.singletonMap("ieOnly", "true");
        SuperBatchPluginResource resource = SuperBatchPluginResource.parse(path, params);
        assertEquals(params, resource.getParams());
        assertEquals(path + "?ieOnly=true", resource.getUrl());
        assertEquals("batch.js", resource.getResourceName());
    }

    @Test
    public void testParseWithParams() {
        String path = "/download/superbatch/js/batch.js";
        Map<String, String> params = new TreeMap<String, String>();
        params.put("ieOnly", "true");
        params.put("zomg", "false");
        SuperBatchPluginResource resource = SuperBatchPluginResource.parse(path, params);
        assertEquals(params, resource.getParams());
        assertEquals(path + "?ieOnly=true&zomg=false", resource.getUrl());
        assertEquals("batch.js", resource.getResourceName());
    }
}
