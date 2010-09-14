package org.maera.plugin.classloader.url;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL stream handler of a byte array
 */
public class BytesUrlStreamHandler extends URLStreamHandler {
    private final byte[] content;

    public BytesUrlStreamHandler(byte[] content) {
        this.content = content;
    }

    public URLConnection openConnection(URL url) {
        return new BytesUrlConnection(url, content);
    }
}
