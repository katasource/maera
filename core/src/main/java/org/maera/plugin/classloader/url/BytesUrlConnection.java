package org.maera.plugin.classloader.url;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * URL connection from a byte array
 */
public class BytesUrlConnection extends URLConnection {
    private final byte[] content;

    public BytesUrlConnection(URL url, byte[] content) {
        super(url);
        this.content = content;
    }

    public void connect() {
    }

    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }
}
