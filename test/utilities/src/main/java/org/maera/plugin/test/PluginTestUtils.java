package org.maera.plugin.test;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *
 */
public class PluginTestUtils {
    public static final String PROJECT_VERSION;
    public static final String SIMPLE_TEST_JAR;
    public static final String INNER1_TEST_JAR;
    public static final String INNER2_TEST_JAR;
    public static final String FILTER_TEST_JAR;

    static {
        PROJECT_VERSION = System.getProperty("project.version");
        SIMPLE_TEST_JAR = "atlassian-plugins-simpletest-" + PROJECT_VERSION + ".jar";
        INNER1_TEST_JAR = "atlassian-plugins-innerjarone-" + PROJECT_VERSION + ".jar";
        INNER2_TEST_JAR = "atlassian-plugins-innerjartwo-" + PROJECT_VERSION + ".jar";
        FILTER_TEST_JAR = "atlassian-plugins-filtertest-" + PROJECT_VERSION + ".jar";
    }

    public static File getFileForResource(final String resourceName) throws URISyntaxException {
        return new File(new URI(PluginTestUtils.class.getClassLoader().getResource(resourceName).toString()));
    }

    public static File createTempDirectory(Class source) throws IOException {
        return createTempDirectory(source.getName());
    }

    public static File createTempDirectory(String name) throws IOException {
        File tmpDir = new File("target" + File.separator + "tmp" + File.separator + name).getAbsoluteFile();
        if (tmpDir.exists()) {
            FileUtils.cleanDirectory(tmpDir);
        } else {
            tmpDir.mkdirs();
        }
        return tmpDir;
    }
}
