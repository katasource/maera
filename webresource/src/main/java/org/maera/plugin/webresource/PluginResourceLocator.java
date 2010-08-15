package org.maera.plugin.webresource;

import org.maera.plugin.servlet.DownloadableResource;

import java.util.List;
import java.util.Map;

/**
 * Assists in locating plugin resources in different ways.
 *
 * @since 2.2
 */
public interface PluginResourceLocator {
    String[] BATCH_PARAMS = new String[]{"ieonly", "media", "content-type", "cache"};

    /**
     * Returns true if this locator can parse the given url.
     */
    boolean matches(String url);

    /**
     * Returns a {@link DownloadableResource} represented by the given url and query params.
     * {@link #matches(String)} should be called before invoking this method. If the url is
     * not understood by the locator, null will be returned.
     */
    DownloadableResource getDownloadableResource(String url, Map<String, String> queryParams);

    /**
     * Returns a list of {@link PluginResource}s for a given plugin module's complete key. If
     * the plugin the module belongs to is not enabled or does not exist, an empty list is returned.
     */
    List<PluginResource> getPluginResources(String moduleCompleteKey);

    /**
     * Constructs and returns url for the given resource.
     * This method is not responsible for adding any static caching url prefixes.
     *
     * @param pluginModuleKey a plugin module's complete key
     * @param resourceName    the name of the resource described in the module
     */
    String getResourceUrl(String pluginModuleKey, String resourceName);
}