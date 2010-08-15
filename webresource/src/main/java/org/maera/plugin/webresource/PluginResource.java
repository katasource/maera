package org.maera.plugin.webresource;

import java.util.Map;

/**
 * Represents a plugin resource.
 *
 * @since 2.2
 */
public interface PluginResource {
    /**
     * @return true if caching for this resource is supported. Use this check to append a static
     *         caching url prefix to this resource's url.
     */
    boolean isCacheSupported();

    /**
     * @return the url for this plugin resource.
     */
    String getUrl();

    /**
     * @return the resource name for the plugin resource.
     */
    String getResourceName();

    /**
     * @return the plugin module's complete key for which this resource belongs to.
     */
    String getModuleCompleteKey();

    /**
     * @return a map of parameter key and value pairs for this resource.
     */
    Map<String, String> getParams();

    /**
     * @return the version prefix string for a cached resource
     */
    String getVersion(WebResourceIntegration integration);
}