package org.maera.plugin;

import java.io.File;
import java.net.URI;

/**
 * Creates plugin artifacts by handling URI's that are files and looking at the file's extension
 *
 * @since 2.1.0
 */
public class DefaultPluginArtifactFactory implements PluginArtifactFactory {
    /**
     * Creates the artifact by looking at the file extension
     *
     * @param artifactUri The artifact URI
     * @return The created artifact
     * @throws IllegalArgumentException If an artifact cannot be created from the URL
     */
    public PluginArtifact create(URI artifactUri) {
        PluginArtifact artifact = null;

        String protocol = artifactUri.getScheme();

        if ("file".equalsIgnoreCase(protocol)) {
            File artifactFile = new File(artifactUri);

            String file = artifactFile.getName();
            if (file.endsWith(".jar"))
                artifact = new JarPluginArtifact(artifactFile);
            else if (file.endsWith(".xml"))
                artifact = new XmlPluginArtifact(artifactFile);
        }

        if (artifact == null)
            throw new IllegalArgumentException("The artifact URI " + artifactUri + " is not a valid plugin artifact");

        return artifact;
    }
}
