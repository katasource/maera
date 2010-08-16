package org.maera.plugin;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.test.PluginJarBuilder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class DefaultPluginArtifactFactoryTest {

    File spaceTestDir;
    File testDir;

    @Before
    public void setUp() throws IOException {
        testDir = makeTempDir("test");
        spaceTestDir = makeTempDir("space test");
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(testDir);
        FileUtils.deleteDirectory(spaceTestDir);

        testDir = null;
        spaceTestDir = null;
    }

    @Test
    public void testCreate() throws IOException {
        doCreationTestInDirectory(testDir);
    }

    @Test
    public void testCreateWithSpaceInArtifactPath() throws IOException {
        doCreationTestInDirectory(spaceTestDir);
    }

    private void doCreationTestInDirectory(File directory) throws IOException {
        File xmlFile = new File(directory, "foo.xml");
        FileUtils.writeStringToFile(xmlFile, "<xml/>");
        File jarFile = new PluginJarBuilder("jar").build(directory);

        DefaultPluginArtifactFactory factory = new DefaultPluginArtifactFactory();

        PluginArtifact jarArt = factory.create(jarFile.toURI());
        assertNotNull(jarArt);
        assertTrue(jarArt instanceof JarPluginArtifact);

        PluginArtifact xmlArt = factory.create(xmlFile.toURI());
        assertNotNull(xmlArt);
        assertTrue(xmlArt instanceof XmlPluginArtifact);

        try {
            factory.create(new File(testDir, "bob.jim").toURI());
            fail("Should have thrown exception");
        } catch (IllegalArgumentException ex) {
            // test passed
        }
    }

    private File makeTempDir(String prefix)
            throws IOException {
        File tempDir = File.createTempFile(prefix, "");
        tempDir.delete();
        tempDir.mkdir();

        return tempDir;
    }
}
