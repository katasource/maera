package org.maera.plugin.util;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class ClassLoaderUtilsTest {

    @Test
    public void testLoadClass() throws ClassNotFoundException {
        assertSame(ClassLoaderUtilsTest.class, ClassLoaderUtils.loadClass(ClassLoaderUtilsTest.class.getName(), this.getClass()));

        try {
            ClassLoaderUtils.loadClass("some.class", null);
            fail("Should have thrown a class not found exception");
        }
        catch (ClassNotFoundException ignored) {

        }
    }
}
