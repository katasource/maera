package org.maera.plugin;

/**
 * Generic plugin exception.
 */
///CLOVER:OFF
public class PluginException extends RuntimeException {
    public PluginException() {
        super();
    }

    public PluginException(String s) {
        super(s);
    }

    public PluginException(Throwable throwable) {
        super(throwable);
    }

    public PluginException(String s, Throwable throwable) {
        super(s, throwable);
    }

}
