package org.maera.plugin.classloader;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Unit Tests for the {@link DelegationClassLoader}.
 */
public class DelegationClassLoaderTest {

    @Test
    public void testCanSetDelegate() {
        DelegationClassLoader dcl = new DelegationClassLoader();
        dcl.setDelegateClassLoader(getClass().getClassLoader());
    }

    @Test(expected = ClassNotFoundException.class)
    public void testCantLoadUnknownClassWithDelegateSet() throws ClassNotFoundException {
        ClassLoader parentClassLoader = DelegationClassLoaderTest.class.getClassLoader();

        DelegationClassLoader classLoader = new DelegationClassLoader();
        classLoader.setDelegateClassLoader(parentClassLoader);
        classLoader.loadClass("not.a.real.class.path.NotARealClass");
    }

    @Test(expected = ClassNotFoundException.class)
    public void testCantLoadUnknownClassWithOutDelegateSet() throws ClassNotFoundException {
        DelegationClassLoader classLoader = new DelegationClassLoader();
        classLoader.loadClass("not.a.real.class.path.NotARealClass");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCantSetNullDelegate() {
        DelegationClassLoader dcl = new DelegationClassLoader();
        dcl.setDelegateClassLoader(null);
    }

    @Test
    public void testLoadSystemClassWithDelegateSet() throws ClassNotFoundException {
        ClassLoader parentClassLoader = DelegationClassLoaderTest.class.getClassLoader();
        DelegationClassLoader classLoader = new DelegationClassLoader();
        classLoader.setDelegateClassLoader(parentClassLoader);
        Class c = classLoader.loadClass("java.lang.String");
        assertNotNull(c);
    }

    @Test
    public void testLoadSystemClassWithOutDelegateSet() throws ClassNotFoundException {
        DelegationClassLoader classLoader = new DelegationClassLoader();
        Class c = classLoader.loadClass("java.lang.String");
        assertNotNull(c);
    }
}
