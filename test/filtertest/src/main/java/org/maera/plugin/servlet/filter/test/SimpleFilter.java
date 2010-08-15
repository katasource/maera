package org.maera.plugin.servlet.filter.test;

import javax.servlet.*;
import java.io.IOException;

public class SimpleFilter implements Filter {
    String name;

    public void init(FilterConfig filterConfig) throws ServletException {
        name = filterConfig.getInitParameter("name");
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        response.getWriter().write("entered: " + name + "\n");
        chain.doFilter(request, response);
        response.getWriter().write("exiting: " + name + "\n");
    }

    public void destroy() {
    }
}
