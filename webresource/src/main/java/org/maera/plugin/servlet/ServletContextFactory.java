package org.maera.plugin.servlet;

import javax.servlet.ServletContext;

/**
 * A factory for providing access to a {@link ServletContext}.
 *
 * @since 2.2
 */
public interface ServletContextFactory {
    /**
     * @return a {@link ServletContext}.
     */
    public ServletContext getServletContext();
}