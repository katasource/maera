package org.maera.plugin.loaders.classloading;

import org.apache.commons.io.FileUtils;
import org.maera.plugin.util.ClassLoaderUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class DirectoryPluginLoaderUtils {
    private static final File TEMP_DIRECTORY = new File("target/plugins-temp");
    private static final String TEST_PLUGIN_DIRECTORY = "ap-plugins";

    /**
     * Copies the test plugins to a new temporary directory and returns that directory.
     */
    public static File copyTestPluginsToTempDirectory() throws IOException {
        File directory = createTemporaryDirectory();
        FileUtils.copyDirectory(getTestPluginsDirectory(), directory);

        // Clean up version control files in case we copied them by mistake.
        FileUtils.deleteDirectory(new File(directory, "CVS"));
        FileUtils.deleteDirectory(new File(directory, ".svn"));

        return directory;
    }

    /**
     * Returns the directory on the classpath where the test plugins live.
     */
    public static File getTestPluginsDirectory() {
        URL url = ClassLoaderUtils.getResource(TEST_PLUGIN_DIRECTORY, DirectoryPluginLoaderUtils.class);
        return new File(url.getFile());
    }

    private static File createTemporaryDirectory() {
        File tempDir = new File(TEMP_DIRECTORY, "plugins-" + randomString(6));
        FileUtils.deleteQuietly(tempDir);
        tempDir.mkdirs();
        return tempDir;
    }

    /**
     * Generate a random string of characters - including numbers
     *
     * @param length the length of the string to return
     * @return a random string of characters
     */
    private static String randomString(int length) {
        StringBuffer b = new StringBuffer(length);

        for (int i = 0; i < length; i++) {
            b.append(randomAlpha());
        }

        return b.toString();
    }

    /**
     * Generate a random character from the alphabet - either a-z or A-Z
     *
     * @return a random alphabetic character
     */
    private static char randomAlpha() {
        int i = (int) (Math.random() * 52);

        if (i > 25)
            return (char) (97 + i - 26);
        else
            return (char) (65 + i);
    }

}
