package org.maera.plugin.osgi.performance;

/**
 * Tests the plugin framework handling restarts correctly
 */
public class TestOsgiCacheFrameworkRestart extends TestOsgiNoCacheFrameworkRestart {
    @Override
    protected void startPluginFramework() throws Exception {
        initPluginManager(prov, factory, "1.0");
    }
}