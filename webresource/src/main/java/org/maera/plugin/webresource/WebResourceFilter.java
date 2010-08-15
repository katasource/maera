package org.maera.plugin.webresource;

/**
 * An interface for filtering web resources. It is used as inputs to {@link WebResourceManager} methods to
 * filter resources that are returned.
 *
 * @since 2.4
 */
public interface WebResourceFilter {
    /**
     * Returns true if this filter supports the inclusion of the given resource.
     *
     * @param name name of the resource
     * @return true if the filter includes this resource, false otherwise
     */
    boolean matches(String resourceName);
}
