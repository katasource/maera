package org.maera.plugin.osgi.bridge;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * Utility methods for event bridge classes
 *
 * @since 0.1
 */
class PluginBundleUtils {
    /**
     * Gets the plugin key from the bundle
     * <p/>
     * WARNING: shamelessly copied from {@link org.maera.plugin.osgi.util.OsgiHeaderUtil}, which can't be imported
     * due to creating a cyclic build dependency.  Ensure these two implementations are in sync.
     *
     * @param bundle The plugin bundle
     * @return The plugin key, cannot be null
     * @since 2.2.1
     */
    static String getPluginKey(Bundle bundle) {
        return getPluginKey(
                bundle.getSymbolicName(),
                bundle.getHeaders().get("Maera-Plugin-Key"),
                bundle.getHeaders().get(Constants.BUNDLE_VERSION)
        );
    }

    private static String getPluginKey(Object bundleName, Object atlKey, Object version) {
        Object key = atlKey;
        if (key == null) {
            key = bundleName + "-" + version;
        }
        return key.toString();

    }
}
