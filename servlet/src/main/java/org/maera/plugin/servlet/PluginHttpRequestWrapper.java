package org.maera.plugin.servlet;

import org.maera.plugin.servlet.descriptors.BaseServletModuleDescriptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * A request wrapper for requests bound for servlets declared in plugins.  Does the necessary path
 * munging for requests so that they look like they are
 * <p/>
 * Also wraps the HttpSession in order to work around the Weblogic Session Attribute serialization problem (see PLUG-515)
 */
public class PluginHttpRequestWrapper extends HttpServletRequestWrapper {
    private final String basePath;
    private HttpServletRequest delegate;

    public PluginHttpRequestWrapper(HttpServletRequest request, BaseServletModuleDescriptor<?> descriptor) {
        super(request);
        this.delegate = request;
        this.basePath = findBasePath(descriptor);
    }

    public String getServletPath() {
        String servletPath = super.getServletPath();
        if (basePath != null) {
            servletPath += basePath;
        }
        return servletPath;
    }

    public String getPathInfo() {
        String pathInfo = super.getPathInfo();
        if (pathInfo != null && basePath != null) {
            if (basePath.equals(pathInfo)) {
                return null;
            } else {
                return pathInfo.substring(basePath.length());
            }
        }
        return pathInfo;
    }

    /**
     * A <a href="http://bluxte.net/blog/2006-03/29-40-33.html">commenter</a> based on the
     * <a href="http://java.sun.com/products/servlet/2.1/html/introduction.fm.html#1499">servlet mapping spec</a>
     * defined the mapping processing as:
     * <p/>
     * <ol>
     * <li>A string beginning with a '/' character and ending with a '/*' postfix is used for path mapping.</li>
     * <li>A string beginning with a'*.' prefix is used as an extension mapping.</li>
     * <li>A string containing only the '/' character indicates the "default" servlet of the application. In this
     * case the servlet path is the request URI minus the context path and the path info is null.</li>
     * <li>All other strings are used for exact matches only.</li>
     * </ol>
     * <p/>
     * To find the base path we're really only interested in the first and last case.  Everything else will just get a
     * null base path.  So we'll iterate through the list of paths specified and for the ones that match (1) above,
     * check if the path info returned by the super class matches.  If it does, we return that base path, otherwise we
     * move onto the next one.
     */
    private String findBasePath(BaseServletModuleDescriptor<?> descriptor) {
        String pathInfo = super.getPathInfo();
        if (pathInfo != null) {
            for (String basePath : descriptor.getPaths()) {
                if (isPathMapping(basePath)) {
                    if (pathInfo.startsWith(getMappingRootPath(basePath))) {
                        return getMappingRootPath(basePath);
                    }
                } else if (basePath.equals(pathInfo)) {
                    // Exact match
                    return basePath;
                }
            }
        }
        return null;
    }

    private boolean isPathMapping(String path) {
        return path.startsWith("/") && path.endsWith("/*");
    }

    private String getMappingRootPath(String pathMapping) {
        return pathMapping.substring(0, pathMapping.length() - 2);
    }

    @Override
    public HttpSession getSession() {
        return this.getSession(true);
    }

    @Override
    public HttpSession getSession(final boolean create) {
        HttpSession session = delegate.getSession(create);
        if (session == null) {
            // The delegate returned a null session - so do we.
            return null;
        } else {
            // Wrap this non-null HttpSession
            return new PluginHttpSessionWrapper(session);
        }
    }
}
