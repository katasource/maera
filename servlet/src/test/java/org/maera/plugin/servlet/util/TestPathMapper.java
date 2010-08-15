package org.maera.plugin.servlet.util;

import junit.framework.TestCase;

public class TestPathMapper extends TestCase {
    /**
     * For more info, see:
     * https://studio.atlassian.com/browse/PLUG-597
     * https://studio.atlassian.com/browse/APL-170
     * https://extranet.atlassian.com/display/~evzijst/2010/06/02/Bypassing+Servlet+Filters+with+Double+Slashes
     * https://studio.atlassian.com/source/cru/CR-PLUG-193#c17487
     * https://studio.atlassian.com/browse/PLUG-605
     */
    public void testDoubleSlashes() {
        final PathMapper pathMapper = new DefaultPathMapper();

        pathMapper.put("key", "/foo/bar*");
        assertEquals("key", pathMapper.get("/foo/bar"));
        assertEquals("key", pathMapper.get("/foo//bar"));
        assertEquals("key", pathMapper.get("/foo///bar"));
        assertEquals("key", pathMapper.get("/foo///bar 2"));
        assertNull(pathMapper.get("/images/ddtree/black spinner/12.png"));
    }

    public void testSlashRemover() {
        final DefaultPathMapper pathMapper = new DefaultPathMapper();

        assertNull(pathMapper.removeRedundantSlashes(null));
        assertEquals("foo", pathMapper.removeRedundantSlashes("foo"));
        assertEquals("foo/bar", pathMapper.removeRedundantSlashes("foo/bar"));
        assertEquals("/", pathMapper.removeRedundantSlashes("/"));
        assertEquals("/", pathMapper.removeRedundantSlashes("//"));
        assertEquals("/", pathMapper.removeRedundantSlashes("///"));
        assertEquals("foo/bar", pathMapper.removeRedundantSlashes("foo//bar"));
        assertEquals("foo/bar", pathMapper.removeRedundantSlashes("foo///bar"));
        assertEquals("foo/bar", pathMapper.removeRedundantSlashes("foo////bar"));
        assertEquals("foo/bar/", pathMapper.removeRedundantSlashes("foo////bar/"));
        assertEquals("/f oo/b/ar/", pathMapper.removeRedundantSlashes("//f oo////b/ar//"));
    }

    public void testRemovePath() {
        final PathMapper pathMapper = new DefaultPathMapper();

        pathMapper.put("foo.bar", "/foo*");
        pathMapper.put("foo.baz", "/bar*");
        assertEquals("foo.bar", pathMapper.get("/foo/bar"));
        assertEquals("foo.baz", pathMapper.get("/bar/foo"));

        pathMapper.put("foo.bar", null);
        assertNull(pathMapper.get("/foo/bar"));
        assertEquals(0, pathMapper.getAll("/foo/bar").size());
        assertEquals("foo.baz", pathMapper.get("/bar/foo"));
    }
}
