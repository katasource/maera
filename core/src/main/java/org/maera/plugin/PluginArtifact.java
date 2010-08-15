package org.maera.plugin;

import java.io.File;
import java.io.InputStream;

/**
 * Allows the retrieval of files and/or an input stream of a plugin artifact. Implementations
 * must allow multiple calls to {@link #getInputStream()}.
 *
 * @see PluginController
 * @since 2.0.0
 */
public interface PluginArtifact {
    /**
     * @return true if the resource exists in this artifact, otherwise false
     * @since 2.2.0
     */
    boolean doesResourceExist(String name);

    /**
     * @return an input stream of the resource specified inside the artifact.  Null if the resource cannot be found.
     * @throws PluginParseException if the there was an exception retrieving the resource from the artifact
     */
    InputStream getResourceAsStream(String name) throws PluginParseException;

    /**
     * @return the original name of the plugin artifact file. Typically used
     *         for persisting it to disk with a meaningful name.
     */
    String getName();

    /**
     * @return an InputStream for the entire plugin artifact. Calling this
     *         multiple times will return a fresh input stream each time.
     */
    InputStream getInputStream();

    /**
     * @return the artifact as a file, or its underlying file if it is already one
     * @since 2.2.0
     */
    File toFile();
}
