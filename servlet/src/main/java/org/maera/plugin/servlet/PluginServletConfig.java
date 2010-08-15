package org.maera.plugin.servlet;

import org.maera.plugin.servlet.descriptors.BaseServletModuleDescriptor;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Instances of the PluginServletConfig are passed to plugins servlet {@link Servlet} init() method.  It provides
 * access to the init parameters defined in the plugin xml as well as the ServletContext shared by other filters and
 * servlets in the plugin.
 */
public final class PluginServletConfig implements ServletConfig {
    private final BaseServletModuleDescriptor<?> descriptor;
    private final ServletContext servletContext;

    public PluginServletConfig(BaseServletModuleDescriptor<?> descriptor, ServletContext servletContext) {
        this.descriptor = descriptor;
        this.servletContext = servletContext;
    }

    public String getServletName() {
        return descriptor.getName();
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public String getInitParameter(String s) {
        return (String) descriptor.getInitParams().get(s);
    }

    public Enumeration getInitParameterNames() {
        return Collections.enumeration(descriptor.getInitParams().keySet());
    }
}