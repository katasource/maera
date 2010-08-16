package org.maera.plugin.webresource;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultWebResourceFilterTest {

    @Test
    public void testMatches() {
        assertTrue(DefaultWebResourceFilter.INSTANCE.matches("foo.css"));
        assertTrue(DefaultWebResourceFilter.INSTANCE.matches("foo.js"));
        assertFalse(DefaultWebResourceFilter.INSTANCE.matches("foo.html"));
    }
}
