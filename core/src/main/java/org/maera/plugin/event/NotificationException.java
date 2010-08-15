package org.maera.plugin.event;

import org.maera.plugin.PluginException;

import java.util.Collections;
import java.util.List;

/**
 * This is used to wrap one or more exceptions thrown by Plugin Event Listeners on receiving an event.
 * <p/>
 * <p> {@link #getAllCauses()} will return a list with all the exceptions that were thrown by the listeners.
 * <p> {@link #getCause()} will return just the first Exception in the list.
 *
 * @since 2.3.0
 */
public class NotificationException extends PluginException {
    private final List<Throwable> allCauses;

    /**
     * Constructs a NotificationException with a single caused by Exception thrown by a Listener.
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              (A <code>null</code> value should never be passed because this exception is only used to wrap other exceptions.)
     * @throws NullPointerException      If a null List is passed.
     * @throws IndexOutOfBoundsException If an empty List is passed.
     */
    public NotificationException(final Throwable cause) {
        super(cause);
        allCauses = Collections.singletonList(cause);
    }

    /**
     * Constructs a NotificationException with a List of the Exceptions that were thrown by the Listeners.
     *
     * @param causes all Exceptions that were thrown by the Listeners.
     *               (the full list will be available by the {@link #getAllCauses()} method;
     *               the {@link #getCause()} method will just return the first cause in the list.
     * @throws NullPointerException      If a null List is passed.
     * @throws IndexOutOfBoundsException If an empty List is passed.
     */
    public NotificationException(final List<Throwable> causes) {
        //noinspection ThrowableResultOfMethodCallIgnored
        super(causes.get(0));
        this.allCauses = Collections.unmodifiableList(causes);
    }

    public List<Throwable> getAllCauses() {
        return allCauses;
    }
}
