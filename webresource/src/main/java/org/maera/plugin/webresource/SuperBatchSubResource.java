package org.maera.plugin.webresource;

import java.util.Map;

/**
 * Represents a plugin resource that is a subordinate of the super batch.
 * <p/>
 * This is typically the case for CSS in the superbatch with relative urls to images. For example:
 * <code>/download/superbatch/css/images/foo.png</code>
 */
public class SuperBatchSubResource extends SuperBatchPluginResource {
    public SuperBatchSubResource(String resourceName, String type, Map<String, String> params) {
        super(resourceName, type, params);
    }

    public static boolean matches(String path) {
        return path.indexOf(URL_PREFIX) != -1;
    }

    public static SuperBatchSubResource parse(String path, Map<String, String> params) {
        String type = getType(path);
        int i = path.indexOf('?');
        if (i != -1) // remove query parameters
        {
            path = path.substring(0, i);
        }
        int startIndex = path.indexOf(URL_PREFIX) + URL_PREFIX.length();
        String resourceName = path.substring(startIndex);
        return new SuperBatchSubResource(resourceName, type, params);
    }
}
