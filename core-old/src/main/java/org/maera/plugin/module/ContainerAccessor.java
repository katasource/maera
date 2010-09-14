package org.maera.plugin.module;

import java.util.Collection;

/**
 * The ContainerAccessor allows access to the underlying plugin container (e.g. spring).
 *
 * @since 2.5.0
 */
public interface ContainerAccessor {
    /**
     * Will ask the container to instantiate a bean of the given class and does inject all constructor defined dependencies.
     * Currently we have only spring as a container that will autowire this bean.
     *
     * @param clazz the Class to instantiate. Cannot be null.
     * @return an instantiated bean.
     */
    <T> T createBean(Class<T> clazz);

    /**
     * Gets all the beans that implement a given interface
     *
     * @param interfaceClass The interface class
     * @param <T>            The target interface type
     * @return A collection of implementations from the plugin's container
     */
    <T> Collection<T> getBeansOfType(Class<T> interfaceClass);
}
