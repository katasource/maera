package org.maera.plugin.servlet.descriptors;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextListener;

/**
 * Provides a way for plugins to declare {@link ServletContextListener}s so they can be notified when the
 * {@link javax.servlet.ServletContext} is created for the plugin.  Implementors need to extend this class and implement the
 * {#link autowireObject} method.
 *
 * @since 2.1.0
 */
public class ServletContextListenerModuleDescriptor extends AbstractModuleDescriptor<ServletContextListener> {
    protected static final Logger log = LoggerFactory.getLogger(ServletContextListenerModuleDescriptor.class);

    /**
     * @param moduleFactory
     * @since 2.5.0
     */
    public ServletContextListenerModuleDescriptor(ModuleFactory moduleFactory) {
        super(moduleFactory);
    }

    @Override
    public ServletContextListener getModule() {
        return moduleFactory.createModule(moduleClassName, this);
    }

}
