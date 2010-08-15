package org.maera.plugin.web.renderer;

public class RendererException extends RuntimeException {
    public RendererException(Throwable cause) {
        super(cause);
    }

    public RendererException(String message) {
        super(message);
    }

    public RendererException(String message, Throwable cause) {
        super(message, cause);
    }
}
