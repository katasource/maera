package org.maera.plugin.servlet;

/**
 * An exception was encountered while trying to write a resource to the client.
 * This is usually an IOException meaning that the client aborted and is rarely
 * interesting in production logs.
 */
public class DownloadException extends Exception {
    public DownloadException(final String message) {
        super(message);
    }

    public DownloadException(final String message, final Exception cause) {
        super(message, cause);
    }

    public DownloadException(final Exception cause) {
        super(cause);
    }
}