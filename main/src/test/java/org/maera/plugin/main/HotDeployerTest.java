package org.maera.plugin.main;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.Test;
import org.maera.plugin.PluginController;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HotDeployerTest {

    @Test
    public void testRun() throws InterruptedException {
        Mock mockController = new Mock(PluginController.class);
        mockController.expectAndReturn("scanForNewPlugins", C.ANY_ARGS, 0);

        HotDeployer deployer = new HotDeployer((PluginController) mockController.proxy(), 1000);
        assertFalse(deployer.isRunning());
        deployer.start();
        Thread.sleep(500);
        assertTrue(deployer.isRunning());
        mockController.verify();
        deployer.stop();
        assertFalse(deployer.isRunning());
    }
}
