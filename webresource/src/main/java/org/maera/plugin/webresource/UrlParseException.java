package org.maera.plugin.webresource;

/**
 * An UrlParseException is thrown when a web resource url cannot be parsed correctly.
 */
public class UrlParseException extends Exception {
    public UrlParseException(String message) {
        super(message);
    }
}
