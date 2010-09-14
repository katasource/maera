package net.maera.osgi.container.impl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test cases for the {@link LifecycleContainer} implementation.
 *
 * @since 0.1
 */
public class LifecycleContainerTest {

    @Test
    public void testDefaultInstance() throws Exception {
        LifecycleContainer container = new LifecycleContainer();
        assertNull(container.getFrameworkFactory());
        assertNull(container.getFramework());
        container.init();
        assertNotNull(container.getFrameworkFactory());
        assertNotNull(container.getFramework());
        container.start(); //normal start
        container.stop();  //normal stop
        container.start(); //restart
        container.start(); //idempotent start after already started
        container.stop();  //normal stop after restart
        container.stop();  //idempotent stop after already stopped
        container.destroy();
        assertNull(container.getFramework());
    }

    /**
     * Tests the case where a container is instantiated and init() is not called before calling start().  Ensures
     * that the system-accessible framework can automatically be created and initialized lazily before start() is
     * called.
     * @throws Exception if the test fails
     */
    @Test
    public void testNonInitializedStart() throws Exception {
        LifecycleContainer container = new LifecycleContainer();
        assertNull(container.getFrameworkFactory());
        assertNull(container.getFramework());
        container.start();
        assertNotNull(container.getFrameworkFactory());
        assertNotNull(container.getFramework());
        container.destroy();
    }
}
