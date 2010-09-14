package org.maera.plugin;

/**
 * Parse plugin exception - thrown from code which must parse a plugin.
 */
public class PluginParseException extends PluginException {
    ///CLOVER:OFF

    public PluginParseException() {
        super();
    }

    public PluginParseException(String s) {
        super(s);
    }

    public PluginParseException(Throwable throwable) {
        super(throwable);
    }

    public PluginParseException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
