/*
 * Copyright (c) 2003 by Atlassian Software Systems Pty. Ltd.
 * All rights reserved.
 */
package org.maera.plugin.util.zip;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;

public class FileUnzipper extends AbstractUnzipper {
    private static final Logger log = LoggerFactory.getLogger(FileUnzipper.class);

    private File zipFile;
    private File destDir;

    public FileUnzipper(File zipFile, File destDir) {
        this.zipFile = zipFile;
        this.destDir = destDir;
    }

    /**
     * Unzips all files in the archive
     *
     * @throws Exception
     */
    public void unzip() throws IOException {
        if ((zipFile == null) || !zipFile.isFile())
            return;

        getStreamUnzipper().unzip();
    }

    public ZipEntry[] entries() throws IOException {
        return getStreamUnzipper().entries();
    }

    /**
     * Specify a specific file inside the archive to extract
     *
     * @param fileName
     */
    public File unzipFileInArchive(String fileName) throws IOException {
        File result = null;

        if ((zipFile == null) || !zipFile.isFile() || !StringUtils.isNotEmpty(fileName))
            return result;

        result = getStreamUnzipper().unzipFileInArchive(fileName);

        if (result == null)
            log.error("The file: " + fileName + " could not be found in the archive: " + zipFile.getAbsolutePath());

        return result;
    }

    private StreamUnzipper getStreamUnzipper() throws FileNotFoundException {
        return new StreamUnzipper(new BufferedInputStream(new FileInputStream(zipFile)), destDir);
    }

}
