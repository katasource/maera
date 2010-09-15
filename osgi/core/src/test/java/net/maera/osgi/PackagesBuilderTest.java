package net.maera.osgi;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 0.1
 */
public class PackagesBuilderTest {

    private static final Logger log = LoggerFactory.getLogger(PackagesBuilderTest.class);

    @Test
    public void testDefault() {
        log.info("Packages built: {}", new PackagesBuilder().withOsgiDefaults().withJdkDefaults() );
    }

}
