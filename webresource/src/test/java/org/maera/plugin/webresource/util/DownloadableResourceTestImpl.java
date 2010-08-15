package org.maera.plugin.webresource.util;

import org.maera.plugin.servlet.DownloadException;
import org.maera.plugin.servlet.DownloadableResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A implementation of {@link org.maera.plugin.servlet.DownloadableResource} for testing purposes
 */
public class DownloadableResourceTestImpl implements DownloadableResource {
    private final String contentType;
    private final String content;


    public DownloadableResourceTestImpl(final String contentType, final String content) {
        this.contentType = contentType;
        this.content = content;
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response) {
        return false;
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response)
            throws DownloadException {
        try {
            writeContent(response.getOutputStream());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void streamResource(final OutputStream out) {
        writeContent(out);
    }

    private void writeContent(final OutputStream out) {
        byte[] bytes = content.getBytes();
        try {
            out.write(bytes);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getContentType() {
        return contentType;
    }
}
