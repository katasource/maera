package net.maera.osgi.container.impl;

import org.junit.Test;

/**
 * @since 0.1
 */
public class DefaultContainerTest {

    @Test
    public void testDefaultConfiguration() throws Exception {
        DefaultContainer container = new DefaultContainer();
        container.init();
        container.start();
        container.stop();
        container.destroy();
    }
}
