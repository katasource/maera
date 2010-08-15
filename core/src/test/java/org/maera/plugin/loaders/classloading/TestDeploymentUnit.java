package org.maera.plugin.loaders.classloading;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class TestDeploymentUnit extends TestCase {
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
