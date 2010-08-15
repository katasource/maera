package org.maera.plugin.webresource;

/**
 * A formatting mode for URLs. Used to specify to {@code WebResourceManager} methods whether to use absolute URLs,
 * relative URLs, or allow the concrete implementation to decide
 *
 * @since 2.3.0
 */
public enum UrlMode {
    /**
     * Absolute URL format, with URL scheme, hostname, port (if non-standard for the scheme), and context path.
     */
    ABSOLUTE,
    /**
     * Relative URL format, containing just the context path.
     */
    RELATIVE,
    /**
     * Unspecified URL format, indicating that either absolute or relative URLs are acceptable.   Implementations are
     * free to determine which mode to use based on any criteria of their choosing. For example, an implementation may
     * choose to return a relative URL if it detects that it is running in the context of an HTTP request, and an
     * absolute URL if it detects that it is not.  Or it may choose to always return an absolute URL, or always return a
     * relative URL.  Callers should only use {@code WebResourceManager.UrlMode#AUTO} when they are sure that either an
     * absolute or a relative URL will be appropriate, and should not rely on any particular observed behavior regarding
     * how this value is interpreted, which may vary across different implementations.
     */
    AUTO
}
