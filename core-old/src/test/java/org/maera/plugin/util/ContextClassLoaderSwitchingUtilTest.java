package org.maera.plugin.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ContextClassLoaderSwitchingUtilTest {

    @Mock
    public ClassLoader newLoader;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
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

    @Test
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
