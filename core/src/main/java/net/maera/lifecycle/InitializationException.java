package net.maera.lifecycle;

/**
 * @since 0.1
 * @author Les Hazlewood
 */
public class InitializationException extends LifecycleException {

    public InitializationException(String message) {
        super(message);
    }

    public InitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public InitializationException(Throwable cause) {
        super(cause);
    }
}
