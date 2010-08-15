package org.maera.plugin.util.zip;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class AbstractUnzipper implements Unzipper {
    protected static Logger log = LoggerFactory.getLogger(FileUnzipper.class);
    protected File destDir;

    protected File saveEntry(InputStream is, ZipEntry entry) throws IOException {
        File file = new File(destDir, entry.getName());

        if (entry.isDirectory()) {
            file.mkdirs();
        } else {
            File dir = new File(file.getParent());
            dir.mkdirs();

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                IOUtils.copy(is, fos);
                fos.flush();
            }
            catch (FileNotFoundException fnfe) {
                log.error("Error extracting a file to '" + destDir + File.separator + entry.getName() + "'. This destination is invalid for writing an extracted file stream to. ");
                return null;
            }
            finally {
                IOUtils.closeQuietly(fos);
            }
        }
        file.setLastModified(entry.getTime());

        return file;
    }

    protected ZipEntry[] entries(ZipInputStream zis) throws IOException {
        List entries = new ArrayList();
        try {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                entries.add(zipEntry);
                zis.closeEntry();
                zipEntry = zis.getNextEntry();
            }
        }
        finally {
            IOUtils.closeQuietly(zis);
        }

        return (ZipEntry[]) entries.toArray(new ZipEntry[entries.size()]);
    }

    public void conditionalUnzip() throws IOException {
        Map<String, Long> zipContentsAndLastModified = new HashMap<String, Long>();

        ZipEntry[] zipEntries = entries();
        for (int i = 0; i < zipEntries.length; i++) {
            zipContentsAndLastModified.put(zipEntries[i].getName(), zipEntries[i].getTime());
        }

        // If the jar contents of the directory does not match the contents of the zip
        // The we will nuke the bundled plugins directory and re-extract.
        Map<String, Long> targetDirContents = getContentsOfTargetDir(destDir);
        if (!targetDirContents.equals(zipContentsAndLastModified)) {
            FileUtils.deleteDirectory(destDir);
            unzip();
        } else {
            if (log.isDebugEnabled())
                log.debug("Target directory contents match zip contents. Do nothing.");
        }
    }

    private Map<String, Long> getContentsOfTargetDir(File dir) {
        // Create filter that lists only jars
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        };


        if (!dir.isDirectory()) {
            return Collections.emptyMap();
        }

        Map<String, Long> targetDirContents = new HashMap<String, Long>();
        for (File child : dir.listFiles()) {
            if (log.isDebugEnabled()) {
                log.debug("Examining entry in zip: " + child);
            }
            targetDirContents.put(child.getName(), child.lastModified());
        }

        return targetDirContents;
    }
}
