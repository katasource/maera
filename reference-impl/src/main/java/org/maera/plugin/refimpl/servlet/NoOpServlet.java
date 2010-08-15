package org.maera.plugin.refimpl.servlet;

import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

/**
 * NoOpServlet is a dummy servlet used only to provide a servlet mapping for url patterns that don't have any. This is
 * necessary as some application servers like WebSphere 6.1.0.5 returns a 404 if there are no mapped servlet before
 * applying filters to the request which could potentially change the URL mapped to a valid servlet. For example, the
 * {@code URLRewriter} filter does this. Hence this dummy servlet should never handle any requests.
 * <p/>
 * If this servlet receives a request, it will simply log all relevant information from the request that may be of help
 * in determining why the request was received, as this would not be the desired result.
 *
 * @since 2.3.6
 */
public class NoOpServlet extends HttpServlet {
    private static final Logger log = Logger.getLogger(NoOpServlet.class);
    private static final String RECEIVED_UNEXPECTED_REQUEST = "NoOpServlet received an unexpected request.";
    private static final String UNABLE_TO_HANDLE_REQUEST =
            "Unable to handle request. Request is not a HttpServletRequest";

    public void service(ServletRequest servletRequest, ServletResponse servletResponse)
            throws ServletException, IOException {
        log.warn(RECEIVED_UNEXPECTED_REQUEST);

        if (!(servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse)) {
            log.error(UNABLE_TO_HANDLE_REQUEST);
            return;
        }
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        //log information that may help in finding out why the request came here
        logRequestInformation(request);

        //Let the user know that there is some error
        response.sendError(404, RECEIVED_UNEXPECTED_REQUEST + " More information is available in the log file.");
    }

    /**
     * Logs relevant information about the request such as the request URL and the query string
     *
     * @param request request to log information about
     */
    private void logRequestInformation(HttpServletRequest request) {
        try {
            log.warn("Request Information");
            log.warn("- Request URL: " + request.getRequestURL());
            log.warn("- Query String: " + (request.getQueryString() == null ? "" : request.getQueryString()));

            log.warn("Request Attributes");
            Enumeration attributeNames = request.getAttributeNames();
            while (attributeNames.hasMoreElements()) {
                String name = (String) attributeNames.nextElement();
                Object attribute = request.getAttribute(name);
                log.warn("- " + name + ": " + (attribute == null ? "null" : attribute.toString()));
            }
        }
        catch (Throwable t) {
            log.error("Error rendering logging information" + t);
        }
    }
}
