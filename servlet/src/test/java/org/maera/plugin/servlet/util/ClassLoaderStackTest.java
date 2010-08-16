package org.maera.plugin.servlet.util;

import org.junit.Test;

import java.net.URISyntaxException;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

@SuppressWarnings({"deprecation"})
public class ClassLoaderStackTest {

    @Test
    public void testPopReturnsPreviousContextClassLoader() throws Exception {
        ClassLoader mainLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader pluginLoader1 = new MockClassLoader();
            ClassLoader pluginLoader2 = new MockClassLoader();

            ClassLoaderStack.push(pluginLoader1);
            assertSame(pluginLoader1, Thread.currentThread().getContextClassLoader());
            ClassLoaderStack.push(pluginLoader2);
            assertSame(pluginLoader2, Thread.currentThread().getContextClassLoader());
            ClassLoader previous = ClassLoaderStack.pop();
            assertSame(pluginLoader2, previous);
            assertSame(pluginLoader1, Thread.currentThread().getContextClassLoader());
            previous = ClassLoaderStack.pop();
            assertSame(pluginLoader1, previous);
            assertSame(mainLoader, Thread.currentThread().getContextClassLoader());
        }
        finally {
            // Clean up in case of error
            Thread.currentThread().setContextClassLoader(mainLoader);
        }
    }

    @Test
    public void testPushAndPopHandleNull() throws Exception {
        ClassLoader mainLoader = Thread.currentThread().getContextClassLoader();
        try {
            // popping with empty stack should return null
            assertNull(ClassLoaderStack.pop());
            // pushing null should be a no-op
            ClassLoaderStack.push(null);
            assertSame(mainLoader, Thread.currentThread().getContextClassLoader());
            assertNull(ClassLoaderStack.pop());
        }
        finally {
            // Clean up in case of error
            Thread.currentThread().setContextClassLoader(mainLoader);
        }
    }

    @Test
    public void testThreadClassLoaderIsReplacedAndRestored() throws URISyntaxException {
        ClassLoader mainLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader pluginLoader1 = new MockClassLoader();
            ClassLoader pluginLoader2 = new MockClassLoader();

            ClassLoaderStack.push(pluginLoader1);
            assertSame(pluginLoader1, Thread.currentThread().getContextClassLoader());
            ClassLoaderStack.push(pluginLoader2);
            assertSame(pluginLoader2, Thread.currentThread().getContextClassLoader());
            ClassLoaderStack.pop();
            assertSame(pluginLoader1, Thread.currentThread().getContextClassLoader());
            ClassLoaderStack.pop();
            assertSame(mainLoader, Thread.currentThread().getContextClassLoader());
        }
        finally {
            Thread.currentThread().setContextClassLoader(mainLoader);
        }
    }

    public static class MockClassLoader extends ClassLoader {
    }
}
