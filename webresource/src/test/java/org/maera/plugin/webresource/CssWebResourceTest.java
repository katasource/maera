package org.maera.plugin.webresource;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class CssWebResourceTest {

    private CssWebResource cssWebResource;

    @Before
    public void setUp() throws Exception {
        cssWebResource = new CssWebResource();
    }

    @Test
    public void testFormatIEResource() {
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master-ie.css";

        Map<String, String> params = new HashMap<String, String>();
        params.put("ieonly", "true");
        params.put("media", "screen");
        assertEquals("<!--[if IE]>\n" +
                "<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" media=\"screen\">\n" +
                "<![endif]-->\n",
                cssWebResource.formatResource(url, params));
    }

    @Test
    public void testFormatResource() {
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master.css";

        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" media=\"all\">\n",
                cssWebResource.formatResource(url, new HashMap<String, String>()));
    }

    @Test
    public void testFormatResourceWithParameters() {
        final String url = "/confluence/download/resources/confluence.web.resources:master-styles/master.css";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("title", "Confluence Master CSS");
        params.put("charset", "utf-8");
        params.put("foo", "bar"); // test invalid parameter

        assertEquals("<link type=\"text/css\" rel=\"stylesheet\" href=\"" + url + "\" title=\"Confluence Master CSS\"" +
                " charset=\"utf-8\" media=\"all\">\n",
                cssWebResource.formatResource(url, params));
    }

    @Test
    public void testMatches() {
        assertTrue(cssWebResource.matches("blah.css"));
        assertFalse(cssWebResource.matches("blah.js"));
    }
}
