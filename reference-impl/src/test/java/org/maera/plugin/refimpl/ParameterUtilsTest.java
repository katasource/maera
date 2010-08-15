package org.maera.plugin.refimpl;

import junit.framework.TestCase;
import org.maera.plugin.webresource.UrlMode;

public class ParameterUtilsTest extends TestCase {
    private static final String CONTEXT_PATH = "/atlassian-plugins-refimpl";
    private static final String BASE_URL = "http://localhost:8080" + CONTEXT_PATH;
    private String previousBaseUrl;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        previousBaseUrl = System.getProperty("baseurl");
        System.setProperty("baseurl", BASE_URL);
    }

    @Override
    protected void tearDown() throws Exception {
        if (previousBaseUrl != null) {
            System.setProperty("baseurl", previousBaseUrl);
        } else {
            System.clearProperty("baseurl");
        }
        super.tearDown();
    }

    public void testBaseUrlWithAbsoluteUrlMode() {
        assertEquals(BASE_URL, ParameterUtils.getBaseUrl(UrlMode.ABSOLUTE));
    }

    public void testBaseUrlWithRelativeUrlMode() {
        assertEquals(CONTEXT_PATH, ParameterUtils.getBaseUrl(UrlMode.RELATIVE));
    }

    public void testBaseUrlWithAutoUrlMode() {
        assertEquals(CONTEXT_PATH, ParameterUtils.getBaseUrl(UrlMode.AUTO));
    }
}
