package org.maera.plugin.util;

import junit.framework.TestCase;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TestContextClassLoaderSwitchingUtil extends TestCase {
    @Mock
    private ClassLoader newLoader;

    public void setUp() {
        initMocks(this);
    }

    public void testSwitchClassLoader() {
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        ContextClassLoaderSwitchingUtil.runInContext(newLoader, new Runnable() {
            public void run() {
                assertEquals(newLoader, Thread.currentThread().getContextClassLoader());
                newLoader.getResource("test");
            }
        });

        // Verify the loader is set back.
        assertEquals(currentLoader, Thread.currentThread().getContextClassLoader());

        // Verify the code was actually called
        verify(newLoader).getResource("test");
    }

    public void testSwitchClassLoaderMultiple() {
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        ContextClassLoaderSwitchingUtil.runInContext(newLoader, new Runnable() {
            public void run() {
                assertEquals(newLoader, Thread.currentThread().getContextClassLoader());
                newLoader.getResource("test");
                final ClassLoader innerLoader = mock(ClassLoader.class);
                ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
                ContextClassLoaderSwitchingUtil.runInContext(innerLoader, new Runnable() {
                    public void run() {
                        assertEquals(innerLoader, Thread.currentThread().getContextClassLoader());
                        innerLoader.getResource("test");
                    }
                });

                // Verify the loader is set back.
                assertEquals(currentLoader, Thread.currentThread().getContextClassLoader());

                // Verify the code was actually called
                verify(newLoader).getResource("test");
            }
        });

        // Verify the loader is set back.
        assertEquals(currentLoader, Thread.currentThread().getContextClassLoader());

        // Verify the code was actually called
        verify(newLoader).getResource("test");
    }
}
