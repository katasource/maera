package net.maera.lifecycle;

import net.maera.MaeraException;

/**
 * Interface indicating that the component should be initialized after it has been instantiated and its dependencies
 * have been configured/injected.
 *
 * @since 0.1
 * @author Les Hazlewood
 */
public interface Initializable {

    /**
     * Initialize the component.  A component is typically initialized only once in its lifetime - after it has been
     * instantiated and all of its dependencies have been configured and/or injected.
     *
     * @throws InitializationException if unable to initialize successfully
     */
    void init() throws InitializationException;
}
