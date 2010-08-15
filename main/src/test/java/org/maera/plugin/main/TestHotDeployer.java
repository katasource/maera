package org.maera.plugin.main;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.PluginController;

public class TestHotDeployer extends TestCase {
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
