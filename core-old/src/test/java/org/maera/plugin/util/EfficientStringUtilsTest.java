package org.maera.plugin.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.maera.plugin.util.EfficientStringUtils.endsWith;

public class EfficientStringUtilsTest {

    @Test
    public void testEndsWith() {
        assertTrue(endsWith("abc", "c"));
        assertTrue(endsWith("foo.xml", ".", "xml"));
        assertTrue(endsWith("foo", "foo"));
    }

    @Test
    public void testEndsWithEmptySuffixes() {
        // Degenerate cases: any string ends with nothing
        assertTrue(endsWith("foo", ""));
        assertTrue(endsWith("", ""));
        assertTrue(endsWith("foo"));
        assertTrue(endsWith(""));
    }

    @Test
    public void testEndsWithNoMatchingSuffix() {
        assertFalse(endsWith("foo", "ooo"));
        assertFalse(endsWith("foo.xml", "."));
    }
}
