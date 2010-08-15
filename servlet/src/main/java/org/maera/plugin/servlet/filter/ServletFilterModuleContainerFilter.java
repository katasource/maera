package org.maera.plugin.servlet.filter;

import org.apache.commons.lang.StringUtils;
import org.maera.plugin.servlet.ServletModuleManager;
import org.maera.plugin.servlet.util.ServletContextServletModuleManagerAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * Applications need to create a concrete subclass of this for use in their filter stack.  This filters responsiblity
 * is to retrieve the filters to be applied to the request from the {@link ServletModuleManager} and build a
 * {@link FilterChain} from them.  Once all the filters in the chain have been applied to the request, this filter
 * returns control to the main chain.
 * <p/>
 * There is one init parameters that must be configured for this filter, the "location" parameter.  It can be one of
 * "top", "middle" or "bottom".  A filter with a "top" location must appear before the filter with a "middle" location
 * which must appear before a filter with a "bottom" location.  Where any other application filters lie in the filter
 * stack is completely up to the application.
 *
 * @since 2.1.0
 */
public class ServletFilterModuleContainerFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ServletFilterModuleContainerFilter.class);

    private FilterConfig filterConfig;
    private FilterLocation location;
    private FilterDispatcherCondition condition;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        location = FilterLocation.parse(filterConfig.getInitParameter("location"));
        String dispatcherCondition = filterConfig.getInitParameter("dispatcher");
        if (StringUtils.isNotBlank(dispatcherCondition)) {
            if (!FilterDispatcherCondition.contains(dispatcherCondition)) {
                throw new ServletException("The dispatcher value must be one of the following only " +
                        Arrays.asList(FilterDispatcherCondition.values()) + " - '" + condition + "' is not a valid value.");
            } else {
                condition = FilterDispatcherCondition.valueOf(dispatcherCondition);
            }
        } else {
            throw new ServletException("The dispatcher init param must be specified and be one of " +
                    Arrays.asList(FilterDispatcherCondition.values()));
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    void doFilter(HttpServletRequest request, HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (getServletModuleManager() == null) {
            log.info("Could not get ServletModuleManager. Skipping filter plugins.");
            chain.doFilter(request, response);
            return;
        }

        final Iterable<Filter> filters = getServletModuleManager().getFilters(location, getUri(request), filterConfig, condition);
        FilterChain pluginFilterChain = new IteratingFilterChain(filters.iterator(), chain);
        pluginFilterChain.doFilter(request, response);
    }

    public void destroy() {
    }

    /**
     * Retrieve the DefaultServletModuleManager from your container framework.  Uses the {@link org.maera.plugin.servlet.util.ServletContextServletModuleManagerAccessor}
     * by default.
     */
    protected ServletModuleManager getServletModuleManager() {
        return ServletContextServletModuleManagerAccessor.getServletModuleManager(filterConfig.getServletContext());
    }

    protected final FilterConfig getFilterConfig() {
        return filterConfig;
    }

    protected final FilterLocation getFilterLocation() {
        return location;
    }

    /**
     * Gets the uri from the request.  Copied from Struts 2.1.0.
     *
     * @param request The request
     * @return The uri
     */
    private static String getUri(HttpServletRequest request) {
        // handle http dispatcher includes.
        String uri = (String) request
                .getAttribute("javax.servlet.include.servlet_path");
        if (uri != null) {
            return uri;
        }

        uri = getServletPath(request);
        if (uri != null && !"".equals(uri)) {
            return uri;
        }

        uri = request.getRequestURI();
        return uri.substring(request.getContextPath().length());
    }

    /**
     * Retrieves the current request servlet path.
     * Deals with differences between servlet specs (2.2 vs 2.3+).
     * Copied from Struts 2.1.0.
     *
     * @param request the request
     * @return the servlet path
     */
    private static String getServletPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();

        String requestUri = request.getRequestURI();
        // Detecting other characters that the servlet container cut off (like anything after ';')
        if (requestUri != null && servletPath != null && !requestUri.endsWith(servletPath)) {
            int pos = requestUri.indexOf(servletPath);
            if (pos > -1) {
                servletPath = requestUri.substring(requestUri.indexOf(servletPath));
            }
        }

        if (null != servletPath && !"".equals(servletPath)) {
            return servletPath;
        }

        int startIndex = request.getContextPath().equals("") ? 0 : request.getContextPath().length();
        int endIndex = request.getPathInfo() == null ? requestUri.length() : requestUri.lastIndexOf(request.getPathInfo());

        if (startIndex > endIndex) { // this should not happen
            endIndex = startIndex;
        }

        return requestUri.substring(startIndex, endIndex);
    }
}
