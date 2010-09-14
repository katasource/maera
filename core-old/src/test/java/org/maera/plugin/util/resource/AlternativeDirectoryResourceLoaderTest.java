package org.maera.plugin.util.resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import static org.junit.Assert.*;

public class AlternativeDirectoryResourceLoaderTest {

    private File base;
    private File kid;

    @Before
    public void setUp() throws IOException {
        base = new File("target");
        if (!base.exists()) {
            base = File.createTempFile("tests", ".tmp");
            base.delete();
            base.mkdir();
            File classes = new File(base, "classes");
            classes.mkdir();
            new File(classes, "com").mkdir();
        }
        kid = new File(base, "kid.txt");
        kid.createNewFile();
    }

    @After
    public void tearDown() throws Exception {
        if (!"target".equals(base.getName())) {
            FileUtils.deleteDirectory(base);
        }
        kid.delete();
    }

    @Ignore
    @Test
    public void testGetResource() throws MalformedURLException {
        try {
            System.setProperty(AlternativeDirectoryResourceLoader.PLUGIN_RESOURCE_DIRECTORIES, base.getAbsolutePath());
            AlternativeResourceLoader loader = new AlternativeDirectoryResourceLoader();
            assertEquals(new File(base, "classes").toURI().toURL(), loader.getResource("classes"));
            assertNull(loader.getResource("asdfasdfasf"));
        }
        finally {
            System.getProperties().remove(AlternativeDirectoryResourceLoader.PLUGIN_RESOURCE_DIRECTORIES);
        }
    }

    @Test
    public void testGetResourceAsStream() throws MalformedURLException {
        InputStream in = null;
        try {
            System.setProperty(AlternativeDirectoryResourceLoader.PLUGIN_RESOURCE_DIRECTORIES, base.getAbsolutePath());
            AlternativeResourceLoader loader = new AlternativeDirectoryResourceLoader();
            in = loader.getResourceAsStream("kid.txt");
            assertNotNull(in);
            assertNull(loader.getResource("asdfasdfasf"));
        }
        finally {
            System.getProperties().remove(AlternativeDirectoryResourceLoader.PLUGIN_RESOURCE_DIRECTORIES);
            IOUtils.closeQuietly(in);
        }
    }

    @Test
    public void testGetResourceNoProperty() throws MalformedURLException {
        AlternativeResourceLoader loader = new AlternativeDirectoryResourceLoader();
        assertNull(loader.getResource("classes"));
        assertNull(loader.getResource("asdfasdfasf"));
    }

    @Ignore
    @Test
    public void testGetResourceWithTwoDefined() throws MalformedURLException {
        try {
            File classesDir = new File(base, "classes");
            System.setProperty(AlternativeDirectoryResourceLoader.PLUGIN_RESOURCE_DIRECTORIES,
                    base.getAbsolutePath() + "," + classesDir.getAbsolutePath());
            AlternativeResourceLoader loader = new AlternativeDirectoryResourceLoader();
            assertEquals(new File(base, "classes").toURI().toURL(), loader.getResource("classes"));
            assertEquals(new File(classesDir, "com").toURI().toURL(), loader.getResource("com"));
            assertNull(loader.getResource("asdfasdfasf"));
        }
        finally {
            System.getProperties().remove(AlternativeDirectoryResourceLoader.PLUGIN_RESOURCE_DIRECTORIES);
        }
    }
}
