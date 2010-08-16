package org.maera.plugin.webresource;

import org.junit.Test;
import org.maera.plugin.servlet.DownloadException;
import org.maera.plugin.servlet.DownloadableResource;
import org.maera.plugin.servlet.util.CapturingHttpServletResponse;
import org.maera.plugin.webresource.util.DownloadableResourceTestImpl;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class BatchPluginResourceTest {

    @Test
    public void testEquals() {
        final String moduleKey = "test.plugin:webresources";
        final String type = "js";

        final Map<String, String> params1 = new TreeMap<String, String>();
        params1.put("key", "value");
        params1.put("foo", "bar");
        final BatchPluginResource batch1 = new BatchPluginResource(moduleKey, type, params1);

        final Map<String, String> params2 = new TreeMap<String, String>();
        params2.put("key", "value");
        params2.put("foo", "bar");
        final BatchPluginResource batch2 = new BatchPluginResource(moduleKey, type, params2);

        final Map<String, String> params3 = new TreeMap<String, String>();
        params3.put("key", "value");
        params3.put("foo", "bart");
        final BatchPluginResource batch3 = new BatchPluginResource(moduleKey, type, params3);

        assertEquals(batch1, batch2);
        assertNotSame(batch1, batch3);
    }

    @Test
    public void testGetUrl() {
        final BatchPluginResource resource = new BatchPluginResource("test.plugin:webresources", "js", Collections.<String, String>emptyMap());
        assertEquals("/download/batch/test.plugin:webresources/test.plugin:webresources.js", resource.getUrl());
    }

    @Test
    public void testGetUrlWithParams() {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("foo", "bar");
        params.put("moo", "cow");

        final BatchPluginResource resource = new BatchPluginResource("test.plugin:webresources", "js", params);
        assertEquals("/download/batch/test.plugin:webresources/test.plugin:webresources.js?foo=bar&moo=cow", resource.getUrl());
    }

    @Test
    public void testIsCacheSupported() throws Exception {
        final BatchPluginResource resource = BatchPluginResource.parse("/download/batch/test.plugin:webresources/test.plugin:webresources.css",
                Collections.<String, String>emptyMap());
        assertTrue(resource.isCacheSupported());

        final Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("cache", "false");
        final BatchPluginResource resource2 = BatchPluginResource.parse("/download/batch/test.plugin:webresources/test.plugin:webresources.css",
                queryParams);
        assertFalse(resource2.isCacheSupported());
    }

    @Test
    public void testNewLineStreamingHttpResponse() throws DownloadException {
        final BatchPluginResource batchResource = new BatchPluginResource("test.plugin:webresources", "js", null);

        final DownloadableResource testResource1 = new DownloadableResourceTestImpl("text/js", "Test1");
        final DownloadableResource testResource2 = new DownloadableResourceTestImpl("text/js", "Test2");
        batchResource.add(testResource1);
        batchResource.add(testResource2);

        final CapturingHttpServletResponse response = new CapturingHttpServletResponse();
        batchResource.serveResource(null, response);

        final String actualResponse = response.toString();
        assertEquals("Test1\nTest2\n", actualResponse);
    }

    @Test
    public void testNewLineStreamingOutputStream() throws DownloadException {
        final BatchPluginResource batchResource = new BatchPluginResource("test.plugin:webresources", "js", null);

        final DownloadableResource testResource1 = new DownloadableResourceTestImpl("text/js", "Test1");
        final DownloadableResource testResource2 = new DownloadableResourceTestImpl("text/js", "Test2");
        batchResource.add(testResource1);
        batchResource.add(testResource2);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        batchResource.streamResource(baos);

        final String actualResponse = baos.toString();
        assertEquals("Test1\nTest2\n", actualResponse);
    }

    @Test
    public void testParse() throws Exception {
        final BatchPluginResource resource = BatchPluginResource.parse("/download/batch/test.plugin:webresources/test.plugin:webresources.css",
                Collections.<String, String>emptyMap());
        assertEquals("css", resource.getType());
        assertEquals("test.plugin:webresources", resource.getModuleCompleteKey());

        final Map<String, String> params = resource.getParams();

        assertEquals(0, params.size());
    }

    @Test(expected = UrlParseException.class)
    public void testParseInvlaidUrlThrowsException() throws UrlParseException {
        BatchPluginResource.parse("/download/batch/blah.css", Collections.<String, String>emptyMap());
    }

    @Test
    public void testParseWithParams() throws Exception {
        final Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("ieonly", "true");
        queryParams.put("foo", "bar");

        final BatchPluginResource resource = BatchPluginResource.parse("/download/batch/test.plugin:webresources/test.plugin:webresources.css",
                queryParams);
        assertEquals("css", resource.getType());
        assertEquals("test.plugin:webresources", resource.getModuleCompleteKey());

        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        params.put("foo", "bar");

        assertEquals(params, resource.getParams());
    }

    @Test
    public void testParseWithParams2() throws Exception {
        final Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("ieonly", "true");
        queryParams.put("foo", "bar");

        final BatchPluginResource resource = BatchPluginResource.parse("/download/batch/test.plugin:webresources/test.plugin:webresources.css?ieonly=true&foo=bar",
                queryParams);
        assertEquals("css", resource.getType());
        assertEquals("test.plugin:webresources", resource.getModuleCompleteKey());

        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        params.put("foo", "bar");

        assertEquals(params, resource.getParams());
    }

    @Test
    public void testParseWithParamsAndRandomPrefix() throws Exception {
        final Map<String, String> queryParams = new TreeMap<String, String>();
        queryParams.put("ieonly", "true");
        queryParams.put("foo", "bar");

        final BatchPluginResource resource = BatchPluginResource.parse("/random/stuff/download/batch/test.plugin:webresources/test.plugin:webresources.css?ieonly=true&foo=bar",
                queryParams);
        assertEquals("css", resource.getType());
        assertEquals("test.plugin:webresources", resource.getModuleCompleteKey());

        final Map<String, String> params = new TreeMap<String, String>();
        params.put("ieonly", "true");
        params.put("foo", "bar");

        assertEquals(params, resource.getParams());
    }

    @Test
    public void testRoundTrip() throws Exception {
        final Map<String, String> params = new TreeMap<String, String>();
        params.put("foo", "bar");

        final String moduleKey = "test.plugin:webresources";
        final BatchPluginResource resource = new BatchPluginResource(moduleKey, "js", params);
        final String url = resource.getUrl();
        final BatchPluginResource parsedResource = BatchPluginResource.parse(url, params);

        assertEquals(resource.getType(), parsedResource.getType());
        assertEquals(resource.getModuleCompleteKey(), parsedResource.getModuleCompleteKey());
        assertEquals(resource.getParams(), parsedResource.getParams());
        assertEquals(moduleKey + ".js", resource.getResourceName());
    }
}
