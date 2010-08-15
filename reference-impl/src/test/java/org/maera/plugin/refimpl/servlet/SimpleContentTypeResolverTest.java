package org.maera.plugin.refimpl.servlet;

import junit.framework.TestCase;

public class SimpleContentTypeResolverTest extends TestCase {
    private static final String JS_URL = "http://example.com/path/to/file.js";
    private static final String CSS_URL = "http://example.com/path/to/file.css";

    private final SimpleContentTypeResolver resolver = new SimpleContentTypeResolver();

    public void testGetContentTypeOfJsUrlReturnsApplicationXJavaScript() {
        assertEquals("application/x-javascript", resolver.getContentType(JS_URL));
    }

    public void testGetContentTypeOfCssUrlReturnsTextCss() {
        assertEquals("text/css", resolver.getContentType(CSS_URL));
    }
}
