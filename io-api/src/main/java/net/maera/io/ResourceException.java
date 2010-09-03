package net.maera.io;

import net.maera.MaeraException;

/**
 * An exception thrown when attempting to acquire, use, or close a {@link Resource}.
 *
 * @since 0.1
 */
public class ResourceException extends MaeraException {

    public ResourceException(String message) {
        super(message);
    }

    public ResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceException(Throwable cause) {
        super(cause);
    }
}
