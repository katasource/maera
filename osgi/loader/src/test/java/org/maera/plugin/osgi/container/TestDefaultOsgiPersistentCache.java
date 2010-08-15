package org.maera.plugin.osgi.container;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.maera.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import org.maera.plugin.test.PluginTestUtils;

import java.io.File;
import java.io.IOException;

public class TestDefaultOsgiPersistentCache extends TestCase {
    private File tmpDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tmpDir = PluginTestUtils.createTempDirectory(TestDefaultOsgiPersistentCache.class);
    }

    public void testRecordLastVersion() throws IOException {
        DefaultOsgiPersistentCache cache = new DefaultOsgiPersistentCache(tmpDir);
        File versionFile = new File(new File(tmpDir, "transformed-plugins"), "cache.key");
        cache.validate("1.0");
        assertTrue(versionFile.exists());
        String txt = FileUtils.readFileToString(versionFile);
        assertEquals("1.0", txt);
    }

    public void testCleanOnUpgrade() throws IOException {
        DefaultOsgiPersistentCache cache = new DefaultOsgiPersistentCache(tmpDir);
        File tmp = File.createTempFile("foo", ".txt", new File(tmpDir, "transformed-plugins"));
        cache.validate("1.0");
        assertTrue(tmp.exists());
        cache.validate("2.0");
        assertFalse(tmp.exists());
    }

    public void testNullVersion() throws IOException {
        DefaultOsgiPersistentCache cache = new DefaultOsgiPersistentCache(tmpDir);
        cache.validate(null);
        File tmp = File.createTempFile("foo", ".txt", new File(tmpDir, "transformed-plugins"));
        assertTrue(tmp.exists());
        cache.validate(null);
        assertTrue(tmp.exists());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.cleanDirectory(tmpDir);
    }
}
