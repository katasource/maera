package org.maera.plugin.loaders.classloading;

import java.io.File;
import java.io.IOException;

public abstract class AbstractClassLoaderTest {
    public static final String PADDINGTON_JAR = "paddington-test-plugin.jar";
    public static final String POOH_JAR = "pooh-test-plugin.jar";

    protected File pluginsDirectory;
    protected File pluginsTestDir;

    protected File getPluginsDirectory() {
        pluginsDirectory = DirectoryPluginLoaderUtils.getTestPluginsDirectory();
        return pluginsDirectory;
    }

    protected void createFillAndCleanTempPluginDirectory() throws IOException {
        pluginsDirectory = DirectoryPluginLoaderUtils.getTestPluginsDirectory();
        pluginsTestDir = DirectoryPluginLoaderUtils.copyTestPluginsToTempDirectory();
    }
}
