package org.maera.plugin.osgi.spring;

import org.maera.plugin.AutowireCapablePlugin;
import org.maera.plugin.module.ContainerAccessor;

/**
 * Allows to access the spring container and access springs beans.
 *
 * @since 2.5.0
 */
public interface SpringContainerAccessor extends ContainerAccessor {
    /**
     * Retrieves a spring bean from the spring bean factory.
     *
     * @param id the id of the spring bean, cannot be null
     * @return the spring bean object
     */
    Object getBean(String id);

    /**
     * 'Autowires' a given object - injects all dependencies defined in the constructor.
     *
     * @param instance the object instance to autowire
     * @param strategy the autowire strategy
     * @deprecated Since 2.5.0, use the createBean method instead.
     */
    @Deprecated
    void autowireBean(Object instance, AutowireCapablePlugin.AutowireStrategy strategy);
}
