package org.maera.plugin.servlet.filter;

import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.servlet.ServletModuleManager;
import org.mockito.Mock;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ServletFilterModuleContainerFilterTest {

    @Mock
    private FilterChain filterChain;
    @Mock
    private ServletModuleManager moduleManager;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testFilter() throws IOException, ServletException {
        when(moduleManager.getFilters(any(FilterLocation.class), eq("/myfilter"), any(FilterConfig.class), eq(FilterDispatcherCondition.REQUEST))).thenReturn(Collections.<Filter>emptyList());

        MyFilter filter = new MyFilter(moduleManager);

        when(request.getContextPath()).thenReturn("/myapp");
        when(request.getRequestURI()).thenReturn("/myapp/myfilter");

        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    public void testFilterNoDispatcher() throws IOException, ServletException {
        when(moduleManager.getFilters(any(FilterLocation.class), eq("/myfilter"), any(FilterConfig.class), eq(FilterDispatcherCondition.REQUEST))).thenReturn(Collections.<Filter>emptyList());

        try {
            new MyFilterNoDispatcher(moduleManager);
            fail("Should have thrown exception on init due to lack of dispatcher value");
        }
        catch (ServletException ex) {
            // this is good
        }
    }

    @Test
    public void testNoServletModuleManager() throws IOException, ServletException {
        MyFilter filter = new MyFilter(null);
        filter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    static class MyFilter extends ServletFilterModuleContainerFilter {

        private final ServletModuleManager moduleManager;
        @Mock
        private FilterConfig filterConfig;

        public MyFilter(ServletModuleManager moduleManager) throws ServletException {
            initMocks(this);
            this.moduleManager = moduleManager;
            when(filterConfig.getInitParameter("location")).thenReturn("after-encoding");
            when(filterConfig.getInitParameter("dispatcher")).thenReturn("REQUEST");
            init(filterConfig);
        }

        protected ServletModuleManager getServletModuleManager() {
            return moduleManager;
        }
    }

    static class MyFilterNoDispatcher extends ServletFilterModuleContainerFilter {

        private final ServletModuleManager moduleManager;
        @Mock
        private FilterConfig filterConfig;

        public MyFilterNoDispatcher(ServletModuleManager moduleManager) throws ServletException {
            initMocks(this);
            this.moduleManager = moduleManager;
            when(filterConfig.getInitParameter("location")).thenReturn("after-encoding");
            init(filterConfig);
        }

        protected ServletModuleManager getServletModuleManager() {
            return moduleManager;
        }
    }
}
