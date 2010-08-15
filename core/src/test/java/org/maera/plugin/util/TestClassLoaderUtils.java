package org.maera.plugin.util;

import junit.framework.TestCase;

public class TestClassLoaderUtils extends TestCase {
    public void testLoadClass() throws ClassNotFoundException {
        assertEquals(TestClassLoaderUtils.class, ClassLoaderUtils.loadClass(TestClassLoaderUtils.class.getName(), this.getClass()));

        try {
            ClassLoaderUtils.loadClass("some.class", null);
            fail("Should have thrown a class not found exception");
        }
        catch (ClassNotFoundException ex) {
            // good, good
        }
    }
}
