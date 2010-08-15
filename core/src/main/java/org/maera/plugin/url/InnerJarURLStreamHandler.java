package org.maera.plugin.url;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 */
public class InnerJarURLStreamHandler extends URLStreamHandler {
    public InnerJarURLStreamHandler() {
    }

    /**
     * @see java.net.URLStreamHandler
     */
    public URLConnection openConnection(URL url) throws IOException {
        return new InnerJarURLConnection(url);
    }

    /**
     * @see java.net.URLStreamHandler
     */
    public void parseURL(URL url, String spec, int start, int limit) {
        String specPath = spec.substring(start,
                limit);

        String urlPath;

        if (specPath.charAt(0) == '/') {
            urlPath = specPath;
        } else if (specPath.charAt(0) == '!') {
            String relPath = url.getFile();

            int bangLoc = relPath.lastIndexOf("!");

            if (bangLoc < 0) {
                urlPath = relPath + specPath;
            } else {
                urlPath = relPath.substring(0,
                        bangLoc) + specPath;
            }
        } else {
            String relPath = url.getFile();

            if (relPath != null) {
                int lastSlashLoc = relPath.lastIndexOf("/");

                if (lastSlashLoc < 0) {
                    urlPath = "/" + specPath;
                } else {
                    urlPath = relPath.substring(0,
                            lastSlashLoc + 1) + specPath;
                }
            } else {
                urlPath = specPath;
            }
        }

        setURL(url, "jar", "", 0, null, null, urlPath, null, null);
    }
}
