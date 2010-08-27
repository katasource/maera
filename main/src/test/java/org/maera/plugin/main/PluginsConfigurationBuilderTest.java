package org.maera.plugin.main;

import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class PluginsConfigurationBuilderTest {

    @Test
    public void testHotDeploySetting() {
        PluginsConfiguration config = new PluginsConfigurationBuilder()
                .hotDeployPollingFrequency(2, TimeUnit.SECONDS)
                .pluginDirectory(new File(System.getProperty("java.io.tmpdir")))
                .build();
        assertEquals(2000, config.getHotDeployPollingPeriod());
    }
}
