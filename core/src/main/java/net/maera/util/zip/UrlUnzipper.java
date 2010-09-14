package net.maera.util.zip;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UrlUnzipper extends AbstractUnzipper {
    private URL zipUrl;

    public UrlUnzipper(URL zipUrl, File destDir) {
        this.zipUrl = zipUrl;
        this.destDir = destDir;
    }

    public void unzip() throws IOException {
        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(zipUrl.openStream());

            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                saveEntry(zis, zipEntry);
            }
        }
        finally {
            IOUtils.closeQuietly(zis);
        }
    }

    public File unzipFileInArchive(String fileName) throws IOException {
        throw new UnsupportedOperationException("Feature not implemented.");
    }

    public ZipEntry[] entries() throws IOException {
        return entries(new ZipInputStream(zipUrl.openStream()));
    }
}
