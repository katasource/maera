package org.maera.plugin.webresource;

import org.maera.plugin.PluginAccessor;

import java.util.Map;

/**
 * The integration layer between Plugin's Web Resource Handler, and specific applications (eg JIRA, Confluence).
 *
 * @see WebResourceManagerImpl#WebResourceManagerImpl(PluginResourceLocator, WebResourceIntegration)
 */
public interface WebResourceIntegration {
    /**
     * Applications must implement this method to get access to the application's PluginAccessor
     */
    PluginAccessor getPluginAccessor();

    /**
     * This must be a thread-local cache that will be accessible from both the page, and the decorator
     */
    Map<String, Object> getRequestCache();

    /**
     * Represents the unique number for this system, which when updated will flush the cache. This should be a number
     * and is generally stored in the global application-properties.
     *
     * @return A string representing the count
     */
    String getSystemCounter();

    /**
     * Represents the last time the system was updated.  This is generally obtained from BuildUtils or similar.
     */
    String getSystemBuildNumber();

    /**
     * Returns the base URL for this application.  This method may return either an absolute or a relative URL.
     * Implementations are free to determine which mode to use based on any criteria of their choosing. For example, an
     * implementation may choose to return a relative URL if it detects that it is running in the context of an HTTP
     * request, and an absolute URL if it detects that it is not.  Or it may choose to always return an absolute URL, or
     * always return a relative URL.  Callers should only use this method when they are sure that either an absolute or
     * a relative URL will be appropriate, and should not rely on any particular observed behavior regarding how this
     * value is interpreted, which may vary across different implementations.
     * <p/>
     * In general, the behavior of this method should be equivalent to calling {@link
     * #getBaseUrl(UrlMode)} with a {@code urlMode} value of {@link
     * UrlMode#AUTO}.
     *
     * @return the string value of the base URL of this application
     */
    String getBaseUrl();

    /**
     * Returns the base URL for this application in either relative or absolute format, depending on the value of {@code
     * urlMode}.
     * <p/>
     * If {@code urlMode == {@link UrlMode#ABSOLUTE}}, this method returns an absolute URL, with URL
     * scheme, hostname, port (if non-standard for the scheme), and context path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#RELATIVE}}, this method returns a relative URL containing
     * just the context path.
     * <p/>
     * If {@code urlMode == {@link UrlMode#AUTO}}, this method may return either an absolute or a
     * relative URL.  Implementations are free to determine which mode to use based on any criteria of their choosing.
     * For example, an implementation may choose to return a relative URL if it detects that it is running in the
     * context of an HTTP request, and an absolute URL if it detects that it is not.  Or it may choose to always return
     * an absolute URL, or always return a relative URL.  Callers should only use {@code
     * WebResourceManager.UrlMode#AUTO} when they are sure that either an absolute or a relative URL will be
     * appropriate, and should not rely on any particular observed behavior regarding how this value is interpreted,
     * which may vary across different implementations.
     *
     * @param urlMode specifies whether to use absolute URLs, relative URLs, or allow the concrete implementation to
     *                decide
     * @return the string value of the base URL of this application
     * @since 2.3.0
     */
    String getBaseUrl(UrlMode urlMode);

    /**
     * This version number is used for caching URL generation, and needs to be incremented every time the contents
     * of the superbatch may have changed. Practically this means updating every time the plugin system state changes
     *
     * @return a version number
     */
    String getSuperBatchVersion();
}
