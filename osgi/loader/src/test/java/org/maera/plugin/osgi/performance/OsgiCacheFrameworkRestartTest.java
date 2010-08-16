package org.maera.plugin.osgi.performance;

/**
 * Tests the plugin framework handling restarts correctly
 */
public class OsgiCacheFrameworkRestartTest extends OsgiNoCacheFrameworkRestartTest {
    @Override
    protected void startPluginFramework() throws Exception {
        initPluginManager(prov, factory, "1.0");
    }
}