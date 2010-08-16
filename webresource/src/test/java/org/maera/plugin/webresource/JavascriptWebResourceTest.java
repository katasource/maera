package org.maera.plugin.webresource;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

public class JavascriptWebResourceTest {

    private JavascriptWebResource javascriptWebResource;

    @Before
    public void setUp() throws Exception {
        javascriptWebResource = new JavascriptWebResource();
    }

    @Test
    public void testFormatResource() {
        final String url = "/confluence/download/resources/confluence.web.resources:ajs/atlassian.js";
        assertEquals("<script type=\"text/javascript\" src=\"" + url + "\" ></script>\n",
                javascriptWebResource.formatResource(url, new HashMap<String, String>()));
    }

    @Test
    public void testMatches() {
        assertTrue(javascriptWebResource.matches("blah.js"));
        assertFalse(javascriptWebResource.matches("blah.css"));
    }
}
