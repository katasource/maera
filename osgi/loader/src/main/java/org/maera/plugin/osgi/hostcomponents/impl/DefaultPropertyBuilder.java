package org.maera.plugin.osgi.hostcomponents.impl;

import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;
import org.maera.plugin.osgi.hostcomponents.PropertyBuilder;

/**
 * Default property builder for host components
 */
class DefaultPropertyBuilder implements PropertyBuilder {
    private Registration registration;

    public DefaultPropertyBuilder(Registration registration) {
        this.registration = registration;
    }

    public PropertyBuilder withName(String name) {
        return withProperty(BEAN_NAME, name);
    }

    public PropertyBuilder withContextClassLoaderStrategy(ContextClassLoaderStrategy strategy) {
        return withProperty(CONTEXT_CLASS_LOADER_STRATEGY, strategy.name());
    }

    public PropertyBuilder withProperty(String name, String value) {
        registration.getProperties().put(name, value);
        return this;
    }
}
