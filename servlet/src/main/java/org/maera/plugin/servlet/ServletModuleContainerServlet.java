package org.maera.plugin.servlet;

import org.maera.plugin.servlet.util.ServletContextServletModuleManagerAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Applications need to create a concrete subclass of this for use in their webapp.  This servlets responsiblity
 * is to retrieve the servlet to be used to serve the request from the {@link ServletModuleManager}.  If no servlet
 * can be found to serve the request, a 404 should be sent back to the client.
 */
public class ServletModuleContainerServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(ServletModuleContainerServlet.class);
    private ServletConfig servletConfig;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.servletConfig = servletConfig;
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (getServletModuleManager() == null) {
            log.error("Could not get ServletModuleManager?");
            response.sendError(500, "Could not get ServletModuleManager.");
            return;
        }

        HttpServlet servlet = getServletModuleManager().getServlet(getPathInfo(request), servletConfig);

        if (servlet == null) {
            log.debug("No servlet found for: " + getRequestURI(request));
            response.sendError(404, "Could not find servlet for: " + getRequestURI(request));
            return;
        }

        try {
            servlet.service(request, response);
        }
        catch (UnavailableException e) // prevent this servlet from unloading itself (PLUG-79)
        {
            log.error(e.getMessage(), e);
            response.sendError(500, e.getMessage());
        }
        catch (ServletException e) {
            log.error(e.getMessage(), e);
            response.sendError(500, e.getMessage());
        }
    }

    /**
     * @return the DefaultServletModuleManager from your container framework.  Uses the {@link org.maera.plugin.servlet.util.ServletContextServletModuleManagerAccessor}
     *         by default.
     */
    protected ServletModuleManager getServletModuleManager() {
        return ServletContextServletModuleManagerAccessor.getServletModuleManager(getServletContext());
    }

    private String getPathInfo(HttpServletRequest request) {
        String pathInfo = (String) request.getAttribute(RequestAttributes.PATH_INFO);
        if (pathInfo == null) {
            pathInfo = request.getPathInfo();
        }
        return pathInfo;
    }

    private String getRequestURI(HttpServletRequest request) {
        String requestURI = (String) request.getAttribute(RequestAttributes.REQUEST_URI);
        if (requestURI == null) {
            requestURI = request.getRequestURI();
        }
        return requestURI;
    }

    private static class RequestAttributes {
        static final String PATH_INFO = "javax.servlet.include.path_info";
        static final String REQUEST_URI = "javax.servlet.include.request_uri";
    }
}