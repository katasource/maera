package org.maera.plugin.servlet.filter;

import org.maera.plugin.servlet.descriptors.ServletFilterModuleDescriptor;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Instances of the PluginFilterConfig are passed to plugins {@link Filter} init() method.  It provides
 * access to the init parameters defined in the plugin xml as well as the ServletContext shared by other filters and
 * servlets in the plugin.
 *
 * @since 2.1.0
 */
public class PluginFilterConfig implements FilterConfig {
    private final ServletFilterModuleDescriptor descriptor;
    private final ServletContext servletContext;

    public PluginFilterConfig(ServletFilterModuleDescriptor descriptor, ServletContext servletContext) {
        this.descriptor = descriptor;
        this.servletContext = servletContext;
    }

    public String getFilterName() {
        return descriptor.getName();
    }

    public String getInitParameter(String name) {
        return descriptor.getInitParams().get(name);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(descriptor.getInitParams().keySet());
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

}
