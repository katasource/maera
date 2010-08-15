package org.maera.plugin.servlet;

import org.maera.plugin.servlet.descriptors.ServletModuleDescriptor;
import org.maera.plugin.servlet.util.ClassLoaderStack;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * We are wrapping the plugins servlet in another servlet so that we can set some things up before
 * the plugins servlet is called. Currently we do the following:
 * <ul>
 * <li>the Threads classloader to the plugins classloader)</li>
 * <li>wrap the request so that path info is right for the servlets</li>
 * </ul>
 */
public class DelegatingPluginServlet extends HttpServlet {
    private final ServletModuleDescriptor descriptor;

    private final HttpServlet servlet;

    public DelegatingPluginServlet(final ServletModuleDescriptor descriptor) {
        this.descriptor = descriptor;
        servlet = descriptor.getModule();
    }

    @Override
    public void service(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try {
            servlet.service(new PluginHttpRequestWrapper(req, descriptor), res);
        }
        finally {
            ClassLoaderStack.pop();
        }
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try {
            servlet.init(config);
        }
        finally {
            ClassLoaderStack.pop();
        }
    }

    @Override
    public void destroy() {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try {
            servlet.destroy();
        }
        finally {
            ClassLoaderStack.pop();
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return servlet.equals(obj);
    }

    @Override
    public String getInitParameter(final String name) {
        return servlet.getInitParameter(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        @SuppressWarnings("unchecked")
        final Enumeration<String> initParameterNames = servlet.getInitParameterNames();
        return initParameterNames;
    }

    @Override
    public ServletConfig getServletConfig() {
        return servlet.getServletConfig();
    }

    @Override
    public ServletContext getServletContext() {
        return servlet.getServletContext();
    }

    @Override
    public String getServletInfo() {
        return servlet.getServletInfo();
    }

    @Override
    public String getServletName() {
        return servlet.getServletName();
    }

    @Override
    public int hashCode() {
        return servlet.hashCode();
    }

    @Override
    public void init() throws ServletException {
        servlet.init();
    }

    @Override
    public void log(final String message, final Throwable t) {
        servlet.log(message, t);
    }

    @Override
    public void log(final String msg) {
        servlet.log(msg);
    }

    @Override
    public String toString() {
        return servlet.toString();
    }

    public ServletModuleDescriptor getModuleDescriptor() {
        return descriptor;
    }

    HttpServlet getDelegatingServlet() {
        return servlet;
    }
}
