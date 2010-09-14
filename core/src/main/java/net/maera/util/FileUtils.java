package net.maera.util;

import net.maera.util.zip.UrlUnzipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class FileUtils {

    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Extract the zip from the URL into the destination directory, but only if the contents haven't already been
     * unzipped.  If the directory contains different contents than the zip, the directory is cleaned out
     * and the files are unzipped.
     *
     * @param zipUrl  The zip url
     * @param destDir The destination directory for the zip contents
     */
    public static void conditionallyExtractZipFile(URL zipUrl, File destDir) {
        try {
            UrlUnzipper unzipper = new UrlUnzipper(zipUrl, destDir);
            unzipper.conditionalUnzip();
        }
        catch (IOException e) {
            log.error("Found " + zipUrl + ", but failed to read file", e);
        }
    }
}
