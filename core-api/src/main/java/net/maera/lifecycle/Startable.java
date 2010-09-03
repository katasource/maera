package net.maera.lifecycle;

/**
 * Interface indicating that component has logic can be started after it has been constructed and initialized.  Unlike
 * the {@link Initializable} interface, a {@link Startable} component can be expected to be {@link #start started}
 * more than once in its lifetime.
 */
public interface Startable {

    /**
     * Starts the underlying component's behavior.
     */
    void start();
}
