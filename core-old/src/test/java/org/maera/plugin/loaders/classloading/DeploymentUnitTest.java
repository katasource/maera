package org.maera.plugin.loaders.classloading;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DeploymentUnitTest {

    @Test
    public void testCompareTo() throws IOException {
        File tmp = File.createTempFile("testDeploymentUnit", ".txt");
        DeploymentUnit unit1 = new DeploymentUnit(tmp);
        DeploymentUnit unit2 = new DeploymentUnit(tmp);
        assertEquals(0, unit1.compareTo(unit2));

        tmp.setLastModified(System.currentTimeMillis() + 1000);
        unit2 = new DeploymentUnit(tmp);
        assertEquals(-1, unit1.compareTo(unit2));
    }
}
