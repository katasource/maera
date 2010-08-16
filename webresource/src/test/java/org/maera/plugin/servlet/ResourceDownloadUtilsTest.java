package org.maera.plugin.servlet;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceDownloadUtilsTest {

    private static final String CACHE_CONTROL = "Cache-Control";
    private static final long TEN_YEARS = 1000L * 60L * 60L * 24L * 365L * 10L;

    @Test
    public void testAddCachingHeadersWithCacheControls() {
        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("setDateHeader", C.ANY_ARGS);
        mockResponse.expect("setHeader", C.args(C.eq(CACHE_CONTROL), C.eq("max-age=" + TEN_YEARS)));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("private")));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("foo")));

        ResourceDownloadUtils.addCachingHeaders((HttpServletResponse) mockResponse.proxy(), "private", "foo");
    }

    @Test
    public void testAddPublicCachingHeaders() {
        Mock mockRequest = new Mock(HttpServletRequest.class);

        Mock mockResponse = new Mock(HttpServletResponse.class);
        mockResponse.expect("setDateHeader", C.ANY_ARGS);
        mockResponse.expect("setHeader", C.args(C.eq(CACHE_CONTROL), C.eq("max-age=" + TEN_YEARS)));
        mockResponse.expect("addHeader", C.args(C.eq(CACHE_CONTROL), C.eq("public")));

        ResourceDownloadUtils.addPublicCachingHeaders((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }
}
