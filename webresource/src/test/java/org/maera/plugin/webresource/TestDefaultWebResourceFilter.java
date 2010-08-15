package org.maera.plugin.webresource;

import junit.framework.TestCase;

public class TestDefaultWebResourceFilter extends TestCase {
    public void testMatches() {
        assertTrue(DefaultWebResourceFilter.INSTANCE.matches("foo.css"));
        assertTrue(DefaultWebResourceFilter.INSTANCE.matches("foo.js"));
        assertFalse(DefaultWebResourceFilter.INSTANCE.matches("foo.html"));
    }
}
