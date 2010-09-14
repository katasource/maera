package net.maera.lifecycle;

/**
 * @since 0.1
 */
public interface Stoppable {

    void stop() throws InterruptedException;
}
