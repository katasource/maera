package net.maera.lifecycle;

/**
 * Interface indicating that the implementing component should be destroyed cleanly after it finished being used
 * by an application.  Note that this is an application-level behavior, and must be performed by the application's
 * management infrastructure (e.g. a lifecycle container such as Spring or Guice).  It will attempt to be called
 * whenever possible, but like Java's finalize method, there is no guarantee this method will be called in all cases
 * (for example, if the application shuts down unexpectedly).  It is meant to provide for 'best effort' cleanup logic
 * if any might be necessary.
 *
 * @since 0.1
 * @author Les Hazlewood
 */
public interface Destroyable {

    /**
     * Destroys or 'cleans up' the implementing instance before it is removed permanently from the application.  This
     * method is usually called only once during a component's lifetime and is meant as a means for any desired
     * 'final' cleanup.
     * <p/>
     * <b>NOTE:</b>  The mechanism managing the component's life cycle will attempt to call this method whenever
     * possible, but there is no guarantee that it will be called in all cases (for example, if the application shuts
     * down unexpectedly).  Logic in this method is expected to be a 'best effort' cleanup if any might be necessary.
     */
    void destroy();
}
