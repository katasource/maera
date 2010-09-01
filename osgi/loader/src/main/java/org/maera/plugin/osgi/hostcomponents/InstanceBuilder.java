package org.maera.plugin.osgi.hostcomponents;

/**
 * Ties a host component registration with a host component instance
 *
 * @since 0.1
 */
public interface InstanceBuilder {
    
    /**
     * Declares the host component instance for the registration.
     *
     * @param instance The object to tie to the registration
     * @return The property builder for assigning properties to the registration
     */
    PropertyBuilder forInstance(Object instance);
}
