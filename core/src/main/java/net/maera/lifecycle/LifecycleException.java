package net.maera.lifecycle;

import net.maera.MaeraException;

/**
 * @since 0.1
 * @author Les Hazlewood
 */
public class LifecycleException extends MaeraException {

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public LifecycleException(Throwable cause) {
        super(cause);
    }
}
