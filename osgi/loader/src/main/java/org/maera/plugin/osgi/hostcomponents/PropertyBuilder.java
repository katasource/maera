package org.maera.plugin.osgi.hostcomponents;

/**
 * Ties properties to the host component registration.  Properties can be set via specific methods or generically
 * via {@link #withProperty(String,String)}
 */
public interface PropertyBuilder {
    /**
     * The name of the host component bean, usually the Spring bean identifier
     */
    String BEAN_NAME = "bean-name";

    /**
     * The context class loader strategy to use for managing the CCL when invoking host component methods
     */
    String CONTEXT_CLASS_LOADER_STRATEGY = "context-class-loader-strategy";

    /**
     * Sets the bean name of the host component
     *
     * @param name The name
     * @return The property builder
     */
    PropertyBuilder withName(String name);

    /**
     * Sets the strategy to use for context classloader management
     *
     * @param strategy The strategy to use
     * @return The property builder
     */
    PropertyBuilder withContextClassLoaderStrategy(ContextClassLoaderStrategy strategy);

    /**
     * Sets an arbitrary property to register with the host component
     *
     * @param name  The property name
     * @param value The property value
     * @return The property builder
     */
    PropertyBuilder withProperty(String name, String value);
}
