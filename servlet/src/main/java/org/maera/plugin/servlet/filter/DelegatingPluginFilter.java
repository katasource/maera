package org.maera.plugin.servlet.filter;

import org.maera.plugin.servlet.PluginHttpRequestWrapper;
import org.maera.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import org.maera.plugin.servlet.util.ClassLoaderStack;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * We wrap the plugins filter so that we can set some things up before the plugins filter is called. Currently we do
 * the following:
 * <ul>
 * <li>set the Threads classloader to the plugins classloader)</li>
 * <li>wrap the request so that path info is right for the filters</li>
 * </ul>
 *
 * @since 2.1.0
 */
public class DelegatingPluginFilter implements Filter {
    private final ServletFilterModuleDescriptor descriptor;
    private final Filter filter;

    public DelegatingPluginFilter(ServletFilterModuleDescriptor descriptor) {
        this.descriptor = descriptor;
        this.filter = descriptor.getModule();
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try {
            filter.init(filterConfig);
        }
        finally {
            ClassLoaderStack.pop();
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try {
            // Reset the classloader during chain execution to prevent plugin's classloader being used for the duration
            // of the request
            FilterChain resetContextClassLoaderChain = new FilterChain() {
                public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                    ClassLoaderStack.pop();
                    try {
                        chain.doFilter(servletRequest, servletResponse);
                    }
                    finally {
                        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
                    }
                }
            };
            filter.doFilter(new PluginHttpRequestWrapper((HttpServletRequest) request, descriptor), response, resetContextClassLoaderChain);
        }
        finally {
            ClassLoaderStack.pop();
        }
    }

    public void destroy() {
        ClassLoaderStack.push(descriptor.getPlugin().getClassLoader());
        try {
            filter.destroy();
        }
        finally {
            ClassLoaderStack.pop();
        }
    }

    public Filter getDelegatingFilter() {
        return filter;
    }
}
