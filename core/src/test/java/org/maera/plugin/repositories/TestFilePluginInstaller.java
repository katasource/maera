package org.maera.plugin.repositories;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.maera.plugin.XmlPluginArtifact;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class TestFilePluginInstaller extends TestCase {
    private File tmpDir;
    private File pluginDir;

    @Override
    protected void setUp() throws Exception {
        tmpDir = new File("target/temp").getAbsoluteFile();
        if (tmpDir.exists()) {
            FileUtils.cleanDirectory(tmpDir);
        }
        tmpDir.mkdirs();
        pluginDir = new File(tmpDir, "plugins");
        pluginDir.mkdir();
    }

    public void testInstallPlugin() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File pluginFile = File.createTempFile("plugin", ".jar", tmpDir);

        installer.installPlugin("foo", new XmlPluginArtifact(pluginFile));
        assertTrue(new File(pluginDir, pluginFile.getName()).exists());
    }

    public void testInstallPluginWithExisting() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File pluginFile = File.createTempFile("plugin", ".jar", pluginDir);
        FileUtils.writeStringToFile(pluginFile, "foo");

        File upgradedFile = new File(tmpDir, pluginFile.getName());
        FileUtils.writeStringToFile(upgradedFile, "bar");

        installer.installPlugin("foo", new XmlPluginArtifact(upgradedFile));
        assertEquals("bar", FileUtils.readFileToString(pluginFile));
        assertTrue(new File(pluginDir, FilePluginInstaller.ORIGINAL_PREFIX + pluginFile.getName()).exists());
    }

    public void testInstallPluginWithExistingOld() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File pluginFile = File.createTempFile("plugin", ".jar", pluginDir);
        FileUtils.writeStringToFile(pluginFile, "foo");

        File upgradedFile = new File(tmpDir, pluginFile.getName());
        FileUtils.writeStringToFile(upgradedFile, "bar");

        File upgraded2File = new File(tmpDir, pluginFile.getName());
        FileUtils.writeStringToFile(upgraded2File, "bar");

        installer.installPlugin("foo", new XmlPluginArtifact(upgradedFile));
        assertEquals("bar", FileUtils.readFileToString(pluginFile));
        assertTrue(new File(pluginDir, FilePluginInstaller.ORIGINAL_PREFIX + pluginFile.getName()).exists());
    }

    public void testRevertInstalledPlugin() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File pluginFile = File.createTempFile("plugin", ".jar", tmpDir);

        installer.installPlugin("foo", new XmlPluginArtifact(pluginFile));

        installer.revertInstalledPlugin("foo");
        assertFalse(new File(pluginDir, pluginFile.getName()).exists());
    }

    public void testRevertInstalledPluginWithOld() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File pluginFile = File.createTempFile("plugin", ".jar", pluginDir);
        FileUtils.writeStringToFile(pluginFile, "foo");

        File upgradedFile = new File(tmpDir, pluginFile.getName());
        FileUtils.writeStringToFile(upgradedFile, "bar");

        installer.installPlugin("foo", new XmlPluginArtifact(upgradedFile));
        assertEquals("bar", FileUtils.readFileToString(pluginFile));
        File oldFile = new File(pluginDir, FilePluginInstaller.ORIGINAL_PREFIX + pluginFile.getName());
        assertTrue(oldFile.exists());

        installer.revertInstalledPlugin("foo");
        assertFalse(oldFile.exists());
        assertEquals("foo", FileUtils.readFileToString(pluginFile));
    }

    public void testRevertUpgradedTwicePlugin() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File pluginFile = File.createTempFile("plugin", ".jar", pluginDir);
        FileUtils.writeStringToFile(pluginFile, "foo");

        File upgradedFile = new File(tmpDir, pluginFile.getName());
        FileUtils.writeStringToFile(upgradedFile, "bar");
        installer.installPlugin("foo", new XmlPluginArtifact(upgradedFile));
        assertEquals("bar", FileUtils.readFileToString(pluginFile));

        File upgraded2File = new File(tmpDir, pluginFile.getName());
        FileUtils.writeStringToFile(upgraded2File, "baz");
        installer.installPlugin("foo", new XmlPluginArtifact(upgraded2File));
        assertEquals("baz", FileUtils.readFileToString(pluginFile));

        installer.revertInstalledPlugin("foo");
        assertEquals("foo", FileUtils.readFileToString(pluginFile));
    }

    public void testRevertInstalledPluginWithTwoPrevious() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File originalFile = File.createTempFile("plugin", ".jar", tmpDir);
        FileUtils.writeStringToFile(originalFile, "foo");
        installer.installPlugin("foo", new XmlPluginArtifact(originalFile));

        File upgradedFile = new File(tmpDir, originalFile.getName());
        FileUtils.writeStringToFile(upgradedFile, "bar");
        installer.installPlugin("foo", new XmlPluginArtifact(upgradedFile));

        File upgraded2File = new File(tmpDir, originalFile.getName());
        FileUtils.writeStringToFile(upgraded2File, "baz");
        installer.installPlugin("foo", new XmlPluginArtifact(upgraded2File));

        File pluginFile = new File(pluginDir, originalFile.getName());
        assertEquals("baz", FileUtils.readFileToString(pluginFile));
        installer.revertInstalledPlugin("foo");
        assertFalse(pluginFile.exists());
    }

    public void testRevertInstalledPluginWithTwoPreviousAndDifferentNames() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File originalFile = File.createTempFile("plugin", ".jar", tmpDir);
        FileUtils.writeStringToFile(originalFile, "foo");
        installer.installPlugin("foo", new XmlPluginArtifact(originalFile));

        File upgradedFile = new File(tmpDir, "bar.jar");
        FileUtils.writeStringToFile(upgradedFile, "bar");
        installer.installPlugin("foo", new XmlPluginArtifact(upgradedFile));

        File upgraded2File = new File(tmpDir, "baz.jar");
        FileUtils.writeStringToFile(upgraded2File, "baz");
        installer.installPlugin("foo", new XmlPluginArtifact(upgraded2File));

        assertTrue(new File(pluginDir, upgraded2File.getName()).exists());
        assertTrue(new File(pluginDir, upgradedFile.getName()).exists());
        assertTrue(new File(pluginDir, originalFile.getName()).exists());
        installer.revertInstalledPlugin("foo");
        assertTrue(new File(pluginDir, upgraded2File.getName()).exists());
        assertTrue(new File(pluginDir, upgradedFile.getName()).exists());
        assertFalse(new File(pluginDir, originalFile.getName()).exists());
    }

    public void testClearBackups() throws IOException {
        FilePluginInstaller installer = new FilePluginInstaller(pluginDir);
        File pluginFile = File.createTempFile("plugin", ".jar", pluginDir);
        FileUtils.writeStringToFile(pluginFile, "foo");

        File upgradedFile = new File(tmpDir, pluginFile.getName());
        FileUtils.writeStringToFile(upgradedFile, "bar");

        installer.installPlugin("foo", new XmlPluginArtifact(upgradedFile));
        assertTrue(new File(pluginDir, FilePluginInstaller.ORIGINAL_PREFIX + pluginFile.getName()).exists());
        installer.clearBackups();
        assertTrue(pluginFile.exists());
        assertFalse(new File(pluginDir, FilePluginInstaller.ORIGINAL_PREFIX + pluginFile.getName()).exists());

    }
}
