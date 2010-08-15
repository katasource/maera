package org.maera.plugin.main;

import junit.framework.TestCase;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class TestPluginsConfigurationBuilder extends TestCase {
    public void testHotDeploySetting() {
        PluginsConfiguration config = new PluginsConfigurationBuilder()
                .hotDeployPollingFrequency(2, TimeUnit.SECONDS)
                .pluginDirectory(new File(System.getProperty("java.io.tmpdir")))
                .build();
        assertEquals(2000, config.getHotDeployPollingPeriod());
    }
}
