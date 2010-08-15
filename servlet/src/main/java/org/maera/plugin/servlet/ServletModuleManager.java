package org.maera.plugin.servlet;

import org.maera.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletModuleDescriptor;
import org.maera.plugin.servlet.filter.FilterDispatcherCondition;
import org.maera.plugin.servlet.filter.FilterLocation;
import org.maera.plugin.servlet.filter.ServletFilterModuleContainerFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;

/**
 * The ServletModuleManager is responsible for servlets and filters - and their servlet contexts - defined in plugins.
 * It is used by instances of the {@link ServletModuleContainerServlet} and {@link ServletFilterModuleContainerFilter}
 * to lookup, create and wrap the filters and servlets defined in plugins.
 * <p/>
 * When the first {@link Filter} or {@link Servlet} is first accessed in a plugin, a new {@link ServletContext} is
 * created for all the modules in the plugin to share.  This is done by wrapping the applications
 * {@link ServletContext}, creating a map of attributes that are local to the plugin that are shadowed by the
 * applications {@link ServletContext} attributes, merging any servlet context init parameters from the plugin and the
 * application, and then running through any {@link ServletContextListener}s defined by the plugin has calling their
 * contextInitialized() methods.
 * <p/>
 * The shadowing of the the plugins {@link ServletContext}s attributes are shadowed by the applications attributes
 * means that if an attribute does not exist in the plugin local attribute map, the applications attributes will be
 * returned.  The plugin is thereby prevented from modifying the base applications context attributes on an application
 * wide scope and can instead only change them, but not remove them, on a local scope.
 * <p/>
 * The init parameters in the plugin will override parameters from the base applications servlet
 * init parameters that have the same name.
 * <p/>
 * During the creation of Filters and Servlets, the {@link FilterConfig} and {@link ServletConfig} provided to
 * Filters and Servlets contain the plugin local {@link ServletContext}, as described above,
 * and provides access to the init parameters defined in the plugin xml for the Filter or Servlet.
 * <p/>
 * After being created, the filters and servlets are wrapped to ensure the the init(), service(), doFilter(),
 * and destroy() methods and other methods defined in the Filter and Servlet interfaces are executed in the plugins
 * {@link ClassLoader}.
 * <p/>
 * The plugins {@link ServletContext} is not destroyed until the plugin is disabled.  It is also at this time that any
 * {@link ServletContextListener}s will have their contextDestroyed() methods called.
 */
public interface ServletModuleManager {
    /**
     * Register a new servlet plugin module.
     *
     * @param descriptor Details of what the servlet class is and the path it should serve.
     */
    void addServletModule(ServletModuleDescriptor descriptor);

    /**
     * Return an instance of the HttpServlet that should be used to serve content matching the provided url path.
     *
     * @param path          Path of the incoming request to serve.
     * @param servletConfig ServletConfig given to the delegating servlet.
     * @return HttpServlet that has been registered to serve up content matching the passed in path.
     * @throws ServletException Thrown if there is a problem initializing the servlet to be returned.
     */
    HttpServlet getServlet(String path, final ServletConfig servletConfig) throws ServletException;

    /**
     * Remove a previously registered servlet plugin module.  Requests that come in on the path described in the
     * descriptor will no longer be served.
     *
     * @param descriptor Details of what servlet module to remove.
     */
    void removeServletModule(ServletModuleDescriptor descriptor);

    /**
     * Register a new filter plugin module.
     *
     * @param descriptor Details of what the filter class is and the path it should serve.
     */
    void addFilterModule(ServletFilterModuleDescriptor descriptor);

    /**
     * Returns the filters that have been registered to filter requests at the specified path matching the location
     * in the filter stack.  The filter dispatcher condition will be set to REQUEST.
     *
     * @param location     Place in the applications filter stack the filters should be applied.
     * @param pathInfo     Path of the incoming request to filter.
     * @param filterConfig FilterConfig given to the delegating filter.
     * @return List of filters to be applied, already sorted by weight
     * @throws ServletException Thrown if there is a problem initializing one of the filters to apply.
     * @deprecated Since 2.5.0, use {@link #getFilters(FilterLocation,String,FilterConfig,FilterDispatcherCondition)} instead
     */
    @Deprecated
    Iterable<Filter> getFilters(FilterLocation location, String pathInfo, FilterConfig filterConfig) throws ServletException;

    /**
     * Returns the filters that have been registered to filter requests at the specified path matching the location
     * in the filter stack and registered for the specific dispatcher condition.
     * <p/>
     *
     * @param location     Place in the applications filter stack the filters should be applied.
     * @param pathInfo     Path of the incoming request to filter.
     * @param filterConfig FilterConfig given to the delegating filter.
     * @param condition    The dispatcher tag that filters have been registered to.  Cannot be null.
     * @return List of filters to be applied, already sorted by weight
     * @throws ServletException Thrown if there is a problem initializing one of the filters to apply.
     * @since 2.5.0
     */
    Iterable<Filter> getFilters(FilterLocation location, String pathInfo, FilterConfig filterConfig, FilterDispatcherCondition condition) throws ServletException;

    /**
     * Remove a previously registered filter plugin module.  Requests that come in on the path described in the
     * descriptor will no longer be served.
     *
     * @param descriptor Details of what filter module to remove.
     */
    void removeFilterModule(ServletFilterModuleDescriptor descriptor);
}
