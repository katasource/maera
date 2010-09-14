package org.maera.plugin.util.resource;

import java.io.InputStream;
import java.net.URL;

/**
 * An alternative resource loader for use by plugins that wish to also support loading resources outside the usual
 * resource loading of the particular plugin type.
 *
 * @since 2.2.0
 */
public interface AlternativeResourceLoader {
    /**
     * Retrieve the URL of the resource from the directories.
     *
     * @param path the name of the resource to be loaded
     * @return The URL to the resource, or null if the resource is not found
     */
    URL getResource(String path);

    /**
     * Load a given resource from the directories.
     *
     * @param name The name of the resource to be loaded.
     * @return An InputStream for the resource, or null if the resource is not found.
     */
    InputStream getResourceAsStream(String name);
}
