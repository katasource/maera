package org.maera.plugin.osgi.container;

import org.maera.plugin.PluginException;

/**
 * Generic wrapper exception for any OSGi-related exceptions
 */
///CLOVER:OFF
public class OsgiContainerException extends PluginException {
    public OsgiContainerException() {
    }

    public OsgiContainerException(String s) {
        super(s);
    }

    public OsgiContainerException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public OsgiContainerException(Throwable throwable) {
        super(throwable);
    }
}
