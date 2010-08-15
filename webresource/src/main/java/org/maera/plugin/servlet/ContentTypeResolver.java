package org.maera.plugin.servlet;

/**
 * Looks up content types for URL paths in an application-specific way. To be implemented in applications using
 * maera-plugins.
 */
public interface ContentTypeResolver {
    /**
     * Returns the content type for the given resource path.
     *
     * @param requestUrl the resource path
     * @return the content type
     */
    String getContentType(String requestUrl);
}
