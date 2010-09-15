package net.maera.felix.container;

import org.junit.Test;

/**
 * @since 0.1
 */
public class FelixContainerTest {

    @Test
    public void testDefault() throws Exception {
        FelixContainer container = new FelixContainer();
        container.init();
        container.start();
        container.stop();
        container.destroy();
    }

}
