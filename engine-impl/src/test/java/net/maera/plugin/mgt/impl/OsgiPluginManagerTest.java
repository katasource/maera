package net.maera.plugin.mgt.impl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test cases for the {@link OsgiPluginManager} implementation.
 */
public class OsgiPluginManagerTest {

    @Test
    public void testDefaultInstance() {
        OsgiPluginManager manager = new OsgiPluginManager();
        manager.init();
        assertNotNull(manager.getFrameworkFactory());
        assertNotNull(manager.getFramework());
        manager.destroy();
        assertNull(manager.getFramework());
    }


}
