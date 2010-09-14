package net.maera;

/**
 * Base exception for all Maera exceptions.
 * 
 * @since 0.1
 */
public class MaeraException extends RuntimeException {

    public MaeraException(String message) {
        super(message);
    }

    public MaeraException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaeraException(Throwable cause) {
        super(cause);
    }
}
