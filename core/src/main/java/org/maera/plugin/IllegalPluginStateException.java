package org.maera.plugin;

/**
 * Thrown when a plugin operation has been attempted when the plugin is in an incompatible state.
 *
 * @since 2.2.0
 */
public class IllegalPluginStateException extends PluginException {
    public IllegalPluginStateException() {
    }

    public IllegalPluginStateException(String s) {
        super(s);
    }

    public IllegalPluginStateException(Throwable throwable) {
        super(throwable);
    }

    public IllegalPluginStateException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
