package org.maera.plugin.osgi.container;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.osgi.container.impl.DefaultOsgiPersistentCache;
import org.maera.plugin.test.PluginTestUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class DefaultOsgiPersistentCacheTest {

    private File tmpDir;

    @Before
    public void setUp() throws Exception {
        tmpDir = PluginTestUtils.createTempDirectory(DefaultOsgiPersistentCacheTest.class);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.cleanDirectory(tmpDir);
    }

    @Test
    public void testCleanOnUpgrade() throws IOException {
        DefaultOsgiPersistentCache cache = new DefaultOsgiPersistentCache(tmpDir);
        File tmp = File.createTempFile("foo", ".txt", new File(tmpDir, "transformed-plugins"));
        cache.validate("1.0");
        assertTrue(tmp.exists());
        cache.validate("2.0");
        assertFalse(tmp.exists());
    }

    @Test
    public void testNullVersion() throws IOException {
        DefaultOsgiPersistentCache cache = new DefaultOsgiPersistentCache(tmpDir);
        cache.validate(null);
        File tmp = File.createTempFile("foo", ".txt", new File(tmpDir, "transformed-plugins"));
        assertTrue(tmp.exists());
        cache.validate(null);
        assertTrue(tmp.exists());
    }

    @Test
    public void testRecordLastVersion() throws IOException {
        DefaultOsgiPersistentCache cache = new DefaultOsgiPersistentCache(tmpDir);
        File versionFile = new File(new File(tmpDir, "transformed-plugins"), "cache.key");
        cache.validate("1.0");
        assertTrue(versionFile.exists());
        String txt = FileUtils.readFileToString(versionFile);
        assertEquals("1.0", txt);
    }
}
