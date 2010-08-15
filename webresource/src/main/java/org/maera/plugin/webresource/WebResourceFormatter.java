package org.maera.plugin.webresource;

import java.util.Map;

/**
 * A formatter to format web resources into HTML.
 * <p/>
 * The {@link #matches(String)} method should be called before calling {@link #formatResource(String, Map)},
 * to ensure correct formatting of the resource.
 */
interface WebResourceFormatter extends WebResourceFilter {
    /**
     * Returns a formatted resource string.
     *
     * @param url        url path to the resource
     * @param parameters a {@link Map} of resource parameters
     * @return a formatted resource {@link String}.
     */
    String formatResource(String url, Map<String, String> parameters);
}
