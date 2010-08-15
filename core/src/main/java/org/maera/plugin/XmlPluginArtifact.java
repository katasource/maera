package org.maera.plugin;

import java.io.*;

/**
 * An XML plugin artifact that is just the atlassian-plugin.xml file
 *
 * @since 2.1.0
 */
public class XmlPluginArtifact implements PluginArtifact {
    private final File xmlFile;

    public XmlPluginArtifact(File xmlFile) {
        this.xmlFile = xmlFile;
    }

    /**
     * Always returns false, since it doesn't make sense for an XML artifact
     */
    public boolean doesResourceExist(String name) {
        return false;
    }

    /**
     * Always returns null, since it doesn't make sense for an XML artifact
     */
    public InputStream getResourceAsStream(String name) throws PluginParseException {
        return null;
    }

    public String getName() {
        return xmlFile.getName();
    }

    /**
     * @return a buffered file input stream of the file on disk. This input stream
     *         is not resettable.
     */
    public InputStream getInputStream() {
        try {
            return new BufferedInputStream(new FileInputStream(xmlFile));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("Could not find XML file for eading: " + xmlFile, e);
        }
    }

    public File toFile() {
        return xmlFile;
    }
}
