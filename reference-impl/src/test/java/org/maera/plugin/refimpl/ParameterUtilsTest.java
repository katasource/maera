package org.maera.plugin.refimpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.webresource.UrlMode;

import static org.junit.Assert.assertEquals;

public class ParameterUtilsTest {

    private static final String CONTEXT_PATH = "/maera-plugins-refimpl";
    private static final String BASE_URL = "http://localhost:8080" + CONTEXT_PATH;
    private String previousBaseUrl;

    @Before
    public void setUp() throws Exception {
        previousBaseUrl = System.getProperty("baseurl");
        System.setProperty("baseurl", BASE_URL);
    }

    @After
    public void tearDown() throws Exception {
        if (previousBaseUrl != null) {
            System.setProperty("baseurl", previousBaseUrl);
        } else {
            System.clearProperty("baseurl");
        }
    }

    @Test
    public void testBaseUrlWithAbsoluteUrlMode() {
        assertEquals(BASE_URL, ParameterUtils.getBaseUrl(UrlMode.ABSOLUTE));
    }

    @Test
    public void testBaseUrlWithAutoUrlMode() {
        assertEquals(CONTEXT_PATH, ParameterUtils.getBaseUrl(UrlMode.AUTO));
    }

    @Test
    public void testBaseUrlWithRelativeUrlMode() {
        assertEquals(CONTEXT_PATH, ParameterUtils.getBaseUrl(UrlMode.RELATIVE));
    }
}
