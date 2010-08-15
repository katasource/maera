package org.maera.plugin.osgi.hostcomponents;

/**
 * The strategy for handling the context class loader for host component method invocations
 */
public enum ContextClassLoaderStrategy {
    /**
     * This strategy ensures the context class loader remains the bundle's class loader.
     */
    USE_PLUGIN,

    /**
     * This strategy ensures the context class loader will be set to the host application's class loader.
     */
    USE_HOST
}
