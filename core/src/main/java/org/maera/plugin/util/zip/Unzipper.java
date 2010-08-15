package org.maera.plugin.util.zip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.ZipEntry;

public interface Unzipper {
    void unzip() throws IOException;

    void conditionalUnzip() throws IOException;

    File unzipFileInArchive(String fileName) throws IOException, FileNotFoundException;

    ZipEntry[] entries() throws IOException;
}
