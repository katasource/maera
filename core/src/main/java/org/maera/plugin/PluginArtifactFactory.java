package org.maera.plugin;

import java.net.URI;

/**
 * Creates a plugin artifact from a URL
 *
 * @since 2.1.0
 */
public interface PluginArtifactFactory {
    /**
     * Creates a plugin artifact
     *
     * @param artifactUri The artifact URI
     * @return The artifact.  Must not return null
     * @throws IllegalArgumentException If the artifact cannot be created
     */
    PluginArtifact create(URI artifactUri) throws IllegalArgumentException;
}
