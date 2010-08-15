package org.maera.plugin.osgi.factory.transform;

/**
 * Generic wrapper exception for all exceptions thrown during plugin transformation
 */
public class PluginTransformationException extends RuntimeException {
    public PluginTransformationException() {
    }

    public PluginTransformationException(String s) {
        super(s);
    }

    public PluginTransformationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public PluginTransformationException(Throwable throwable) {
        super(throwable);
    }
}
