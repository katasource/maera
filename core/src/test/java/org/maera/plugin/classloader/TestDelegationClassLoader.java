package org.maera.plugin.classloader;

import junit.framework.TestCase;

/**
 * Unit Tests for the {@link DelegationClassLoader}.
 */
public class TestDelegationClassLoader extends TestCase {

    public void testLoadSystemClassWithOutDelegateSet() throws ClassNotFoundException {
        DelegationClassLoader classLoader = new DelegationClassLoader();
        Class c = classLoader.loadClass("java.lang.String");
        assertNotNull(c);
    }

    public void testLoadSystemClassWithDelegateSet() throws ClassNotFoundException {
        ClassLoader parentClassLoader = TestDelegationClassLoader.class.getClassLoader();
        DelegationClassLoader classLoader = new DelegationClassLoader();
        classLoader.setDelegateClassLoader(parentClassLoader);
        Class c = classLoader.loadClass("java.lang.String");
        assertNotNull(c);
    }

    public void testCantLoadUnknownClassWithOutDelegateSet() {
        DelegationClassLoader classLoader = new DelegationClassLoader();
        try {
            classLoader.loadClass("not.a.real.class.path.NotARealClass");
            fail("ClassNotFoundException expected");
        }
        catch (ClassNotFoundException e) {
            // expected case
        }
    }

    public void testCantLoadUnknownClassWithDelegateSet() {
        ClassLoader parentClassLoader = TestDelegationClassLoader.class.getClassLoader();
        DelegationClassLoader classLoader = new DelegationClassLoader();
        classLoader.setDelegateClassLoader(parentClassLoader);
        try {
            classLoader.loadClass("not.a.real.class.path.NotARealClass");
            fail("ClassNotFoundException expected");
        }
        catch (ClassNotFoundException e) {
            // expected case
        }
    }

    public void testCantSetNullDelegate() {
        DelegationClassLoader dcl = new DelegationClassLoader();
        try {
            dcl.setDelegateClassLoader(null);
            fail("expected IllegalArgumentException with null delegate");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testCanSetDelegate() {
        DelegationClassLoader dcl = new DelegationClassLoader();
        dcl.setDelegateClassLoader(getClass().getClassLoader());
    }

}
