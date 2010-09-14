package org.maera.plugin.util.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads resources from directories configured via the system property {@code plugin.resource.directories}, which should
 * be a comma-delimited list of file paths that contain resources to load.
 *
 * @since 2.2.0
 */
public class AlternativeDirectoryResourceLoader implements AlternativeResourceLoader {
    private final List<File> resourceDirectories;
    private static final Logger log = LoggerFactory.getLogger(AlternativeDirectoryResourceLoader.class);
    public static final String PLUGIN_RESOURCE_DIRECTORIES = "plugin.resource.directories";

    public AlternativeDirectoryResourceLoader() {
        String dirs = System.getProperty(PLUGIN_RESOURCE_DIRECTORIES);
        List<File> tmp = new ArrayList<File>();
        if (dirs != null) {
            for (String dir : dirs.split(",")) {
                File file = new File(dir);
                if (file.exists()) {
                    log.info("Found alternative resource directory " + dir);
                    tmp.add(file);
                } else {
                    log.warn("Resource directory " + dir + ", which resolves to " + file.getAbsolutePath() + " does not exist");
                }
            }
        }

        resourceDirectories = Collections.unmodifiableList(tmp);
    }

    /**
     * Retrieve the URL of the resource from the directories.
     *
     * @param path the name of the resource to be loaded
     * @return The URL to the resource, or null if the resource is not found
     */
    public URL getResource(String path) {
        for (File dir : resourceDirectories) {
            File file = new File(dir, path);
            if (file.exists()) {
                try {
                    return file.toURI().toURL();
                }
                catch (MalformedURLException e) {
                    log.error("Malformed URL: " + file.toString(), e);
                }
            } else {
                log.debug("File " + file + " not found, ignoring");
            }
        }
        return null;
    }

    /**
     * Load a given resource from the directories.
     *
     * @param name The name of the resource to be loaded.
     * @return An InputStream for the resource, or null if the resource is not found.
     */
    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        if (url != null) {
            try {
                return url.openStream();
            }
            catch (IOException e) {
                log.error("Unable to open URL " + url, e);
            }
        }
        return null;
    }

    public List<File> getResourceDirectories() {
        return resourceDirectories;
    }


}
