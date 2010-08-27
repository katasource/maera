package org.maera.plugin.servlet;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.webresource.PluginResourceLocator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class PluginResourceDownloadTest {

    private static final String BATCH_RESOURCE = "/download/batch/js/org.maera.plugin:bar-resources.js";
    private static final String JS_CONTENT_TYPE = "text/javascript";

    private static final String SINGLE_RESOURCE = "/download/resources/org.maera.plugin:foo-resources/foo.js";
    private Mock mockContentTypeResolver;
    private Mock mockPluginResourceLocator;

    private PluginResourceDownload pluginResourceDownload;

    @Before
    public void setUp() throws Exception {
        mockPluginResourceLocator = new Mock(PluginResourceLocator.class);
        mockContentTypeResolver = new Mock(ContentTypeResolver.class);

        pluginResourceDownload = new PluginResourceDownload((PluginResourceLocator) mockPluginResourceLocator.proxy(),
                (ContentTypeResolver) mockContentTypeResolver.proxy(), "UTF-8");
    }

    @After
    public void tearDown() throws Exception {
        mockContentTypeResolver = null;
        mockPluginResourceLocator = null;
        pluginResourceDownload = null;
    }

    @Test
    public void testMatches() {
        mockPluginResourceLocator.expectAndReturn("matches", C.args(C.eq(SINGLE_RESOURCE)), true);
        assertTrue(pluginResourceDownload.matches(SINGLE_RESOURCE));

        mockPluginResourceLocator.expectAndReturn("matches", C.args(C.eq(BATCH_RESOURCE)), true);
        assertTrue(pluginResourceDownload.matches(BATCH_RESOURCE));
    }

    @Test
    public void testResourceNotFound() throws Exception {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getRequestURI", SINGLE_RESOURCE);
        mockRequest.expectAndReturn("getParameterMap", Collections.EMPTY_MAP);

        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("sendError", C.args(C.eq(HttpServletResponse.SC_NOT_FOUND)));
        mockPluginResourceLocator.expectAndReturn("getDownloadableResource", C.args(C.eq(SINGLE_RESOURCE), C.eq(Collections.EMPTY_MAP)), null);

        pluginResourceDownload.serveFile((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }

    @Test
    public void testServeFile() throws Exception {
        Map<String, String[]> params = new HashMap<String, String[]>();
        params.put("aaa", new String[]{"bbb"});

        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getRequestURI", SINGLE_RESOURCE);
        mockRequest.expectAndReturn("getParameterMap", params);

        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("setContentType", C.args(C.eq(JS_CONTENT_TYPE)));

        Mock mockDownloadableResource = new Mock(DownloadableResource.class);
        mockDownloadableResource.expectAndReturn("isResourceModified", C.args(C.eq(mockRequest.proxy()), C.eq(mockResponse.proxy())), false);
        mockDownloadableResource.expectAndReturn("getContentType", JS_CONTENT_TYPE);
        mockDownloadableResource.expect("serveResource", C.args(C.eq(mockRequest.proxy()), C.eq(mockResponse.proxy())));
        mockPluginResourceLocator.expectAndReturn("getDownloadableResource", C.ANY_ARGS, mockDownloadableResource.proxy());

        pluginResourceDownload.serveFile((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }

    @Test
    public void testServeFileContentTypeUsesContentTypeResolverWhenResourceContentTypeIsNull() throws DownloadException {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getRequestURI", SINGLE_RESOURCE);
        mockRequest.expectAndReturn("getParameterMap", Collections.EMPTY_MAP);

        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("setContentType", C.args(C.eq(JS_CONTENT_TYPE)));

        Mock mockDownloadableResource = new Mock(DownloadableResource.class);
        mockDownloadableResource.expectAndReturn("isResourceModified",
                C.args(C.eq(mockRequest.proxy()), C.eq(mockResponse.proxy())),
                false);
        mockDownloadableResource.expectAndReturn("getContentType", null);
        mockDownloadableResource.expect("serveResource", C.args(C.eq(mockRequest.proxy()), C.eq(mockResponse.proxy())));
        mockPluginResourceLocator.expectAndReturn("getDownloadableResource",
                C.ANY_ARGS,
                mockDownloadableResource.proxy());

        mockContentTypeResolver.expectAndReturn("getContentType", C.args(C.eq(SINGLE_RESOURCE)), JS_CONTENT_TYPE);

        pluginResourceDownload.serveFile((HttpServletRequest) mockRequest.proxy(),
                (HttpServletResponse) mockResponse.proxy());

        mockResponse.verify();
    }

    @Test
    public void testServeFileDoesNotCallSetContentTypeWithNullContentType() throws DownloadException {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getRequestURI", SINGLE_RESOURCE);
        mockRequest.expectAndReturn("getParameterMap", Collections.EMPTY_MAP);

        Mock mockResponse = new Mock(HttpServletResponse.class);

        Mock mockDownloadableResource = new Mock(DownloadableResource.class);
        mockDownloadableResource.expectAndReturn("isResourceModified",
                C.args(C.eq(mockRequest.proxy()), C.eq(mockResponse.proxy())),
                false);
        mockDownloadableResource.expectAndReturn("getContentType", null);
        mockDownloadableResource.expect("serveResource", C.args(C.eq(mockRequest.proxy()), C.eq(mockResponse.proxy())));
        mockPluginResourceLocator.expectAndReturn("getDownloadableResource",
                C.ANY_ARGS,
                mockDownloadableResource.proxy());

        mockContentTypeResolver.expectAndReturn("getContentType", C.args(C.eq(SINGLE_RESOURCE)), null);

        pluginResourceDownload.serveFile((HttpServletRequest) mockRequest.proxy(),
                (HttpServletResponse) mockResponse.proxy());
    }
}
