package org.maera.plugin;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two plugins by their names
 */
class PluginNameComparator implements Comparator<Plugin>, Serializable {
    static final long serialVersionUID = -2595168544386708474L;

    /**
     * Gets names of the two given plugins and returns the result of their comparison
     *
     * @param p1 plugin to compare
     * @param p2 plugin to compare
     * @return result of plugin name comparison
     */
    public int compare(final Plugin p1, final Plugin p2) {
        final String name1 = p1.getName();
        final String name2 = p2.getName();
        if ((name1 == null) || (name2 == null)) {
            return (name1 == null) ? (name2 == null) ? 0 : -1 : 1;
        }
        return name1.compareTo(name2);
    }
}
