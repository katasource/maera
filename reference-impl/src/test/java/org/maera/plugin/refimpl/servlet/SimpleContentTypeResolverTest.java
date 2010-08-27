package org.maera.plugin.refimpl.servlet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SimpleContentTypeResolverTest {

    private static final String CSS_URL = "http://example.com/path/to/file.css";
    private static final String JS_URL = "http://example.com/path/to/file.js";

    private final SimpleContentTypeResolver resolver = new SimpleContentTypeResolver();

    @Test
    public void testGetContentTypeOfCssUrlReturnsTextCss() {
        assertEquals("text/css", resolver.getContentType(CSS_URL));
    }

    @Test
    public void testGetContentTypeOfJsUrlReturnsApplicationXJavaScript() {
        assertEquals("application/x-javascript", resolver.getContentType(JS_URL));
    }
}
