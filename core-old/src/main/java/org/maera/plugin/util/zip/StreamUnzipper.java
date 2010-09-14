package org.maera.plugin.util.zip;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Stream based ZIP extractor
 */
public class StreamUnzipper extends AbstractUnzipper {
    private ZipInputStream zis;

    /**
     * Construct a stream unzipper
     *
     * @param zipStream Inputstream to use for ZIP archive reading
     * @param destDir   Directory to unpack stream contents
     */
    public StreamUnzipper(InputStream zipStream, File destDir) {
        if (zipStream == null)
            throw new IllegalArgumentException("zip stream cannot be null");
        this.zis = new ZipInputStream(zipStream);
        this.destDir = destDir;
    }

    public void unzip() throws IOException {
        ZipEntry zipEntry = zis.getNextEntry();
        try {
            while (zipEntry != null) {
                saveEntry(zis, zipEntry);
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
        finally {
            IOUtils.closeQuietly(zis);
        }
    }

    public File unzipFileInArchive(String fileName) throws IOException {
        File result = null;

        try {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                String entryName = zipEntry.getName();

                // os-dependent zips contain a leading back slash "\" character. we want to strip this off first
                if (StringUtils.isNotEmpty(entryName) && entryName.startsWith("/"))
                    entryName = entryName.substring(1);

                if (fileName.equals(entryName)) {
                    result = saveEntry(zis, zipEntry);
                    break;
                }
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
        finally {
            IOUtils.closeQuietly(zis);
        }

        return result;
    }

    public ZipEntry[] entries() throws IOException {
        return entries(zis);
    }
}
