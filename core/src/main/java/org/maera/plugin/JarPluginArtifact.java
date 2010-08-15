package org.maera.plugin;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;

import java.io.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * The implementation of PluginArtifact that is backed by a jar file.
 *
 * @see PluginArtifact
 * @since 2.0.0
 */
public class JarPluginArtifact implements PluginArtifact {
    private final File jarFile;

    public JarPluginArtifact(File jarFile) {
        Validate.notNull(jarFile);
        this.jarFile = jarFile;
    }

    public boolean doesResourceExist(String name) {
        InputStream in = null;
        try {
            in = getResourceAsStream(name);
            return (in != null);
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * @return an input stream for the this file in the jar. Closing this stream also closes the jar file this stream comes from.
     */
    public InputStream getResourceAsStream(String fileName) throws PluginParseException {
        Validate.notNull(fileName, "The file name must not be null");
        final JarFile jar;
        try {
            jar = new JarFile(jarFile);
        }
        catch (IOException e) {
            throw new PluginParseException("Cannot open JAR file for reading: " + jarFile, e);
        }

        ZipEntry entry = jar.getEntry(fileName);
        if (entry == null) {
            return null;
        }

        InputStream descriptorStream;
        try {
            descriptorStream = new BufferedInputStream(jar.getInputStream(entry)) {

                // because we do not expose a handle to the jar file this stream is associated with, we need to make sure
                // we explicitly close the jar file when we're done with the stream (else we'll have a file handle leak)
                public void close() throws IOException {
                    super.close();
                    jar.close();
                }
            };
        }
        catch (IOException e) {
            throw new PluginParseException("Cannot retrieve " + fileName + " from plugin JAR [" + jarFile + "]", e);
        }
        return descriptorStream;
    }

    public String getName() {
        return jarFile.getName();
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * @return a buffered file input stream of the file on disk. This input stream
     *         is not resettable.
     */
    public InputStream getInputStream() {
        try {
            return new BufferedInputStream(new FileInputStream(jarFile));
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException("Could not open JAR file for reading: " + jarFile, e);
        }
    }

    public File toFile() {
        return jarFile;
    }
}
