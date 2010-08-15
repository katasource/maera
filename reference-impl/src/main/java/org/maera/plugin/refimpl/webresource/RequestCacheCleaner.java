package org.maera.plugin.refimpl.webresource;

import org.maera.plugin.refimpl.ContainerManager;
import org.maera.plugin.webresource.WebResourceIntegration;

import javax.servlet.*;
import java.io.IOException;

public class RequestCacheCleaner implements Filter {
    private final WebResourceIntegration webResourceIntegration;

    public RequestCacheCleaner() {
        this.webResourceIntegration = ContainerManager.getInstance().getWebResourceIntegration();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        }
        finally {
            webResourceIntegration.getRequestCache().clear();
        }
    }

    public void init(FilterConfig arg0) throws ServletException {
    }

    public void destroy() {
    }
}
