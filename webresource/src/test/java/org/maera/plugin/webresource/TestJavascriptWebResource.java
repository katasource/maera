package org.maera.plugin.webresource;

import junit.framework.TestCase;

import java.util.HashMap;

public class TestJavascriptWebResource extends TestCase {
    private JavascriptWebResource javascriptWebResource;

    protected void setUp() throws Exception {
        super.setUp();
        javascriptWebResource = new JavascriptWebResource();
    }

    protected void tearDown() throws Exception {
        javascriptWebResource = null;
        super.tearDown();
    }

    public void testMatches() {
        assertTrue(javascriptWebResource.matches("blah.js"));
        assertFalse(javascriptWebResource.matches("blah.css"));
    }

    public void testFormatResource() {
        final String url = "/confluence/download/resources/confluence.web.resources:ajs/atlassian.js";
        assertEquals("<script type=\"text/javascript\" src=\"" + url + "\" ></script>\n",
                javascriptWebResource.formatResource(url, new HashMap<String, String>()));
    }
}
