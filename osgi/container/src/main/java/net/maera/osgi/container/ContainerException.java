package net.maera.osgi.container;

import net.maera.MaeraException;

/**
 * @since 0.1
 */
public class ContainerException extends MaeraException {

    public ContainerException(String message) {
        super(message);
    }

    public ContainerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContainerException(Throwable cause) {
        super(cause);
    }
}
