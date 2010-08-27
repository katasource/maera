package org.maera.plugin.servlet;

import com.mockobjects.dynamic.AnyConstraintMatcher;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.Test;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.servlet.descriptors.ServletModuleDescriptor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ServletModuleContainerServletTest {

    @Test
    public void testIncludedServletDispatchesCorrectly() throws IOException, ServletException {
        final Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.matchAndReturn("getPathInfo", "/original");
        mockHttpServletRequest.expectAndReturn("getAttribute", "javax.servlet.include.path_info", "/included");
        final Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);

        final MockHttpServlet originalServlet = new MockHttpServlet();
        final MockHttpServlet includedServlet = new MockHttpServlet();

        final ServletModuleManager servletModuleManager =
                new DefaultServletModuleManager(new DefaultPluginEventManager()) {

                    @Override
                    public HttpServlet getServlet(String path, ServletConfig servletConfig) throws ServletException {
                        if (path.equals("/original")) {
                            return originalServlet;
                        } else if (path.equals("/included")) {
                            return includedServlet;
                        }
                        return null;
                    }
                };

        final ServletModuleContainerServlet servlet = new ServletModuleContainerServlet() {

            @Override
            protected ServletModuleManager getServletModuleManager() {
                return servletModuleManager;
            }
        };

        servlet.service((HttpServletRequest) mockHttpServletRequest.proxy(),
                (HttpServletResponse) mockHttpServletResponse.proxy());

        assertTrue("includedServlet should have been invoked", includedServlet.wasCalled);
        assertFalse("originalServlet should not have been invoked", originalServlet.wasCalled);
    }

    // ensure that an UnavailableException thrown in the plugin servlet doesn't unload this servlet
    @Test
    public void testServletDoesntUnloadItself() throws IOException, ServletException {
        Mock mockServletModuleManager = new Mock(ServletModuleManager.class);
        Mock mockModuleClassFactory = new Mock(ModuleFactory.class);
        mockModuleClassFactory.expectAndReturn("createModule", new AnyConstraintMatcher(), null);
        ServletModuleDescriptor servletModuleDescriptor = new ServletModuleDescriptor((ModuleFactory) mockModuleClassFactory.proxy(), (ServletModuleManager) mockServletModuleManager.proxy());


        final DelegatingPluginServlet delegatingPluginServlet = new DelegatingPluginServlet(servletModuleDescriptor) {

            public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException {
                throw new UnavailableException("Error in plugin servlet");
            }
        };

        final ServletModuleManager servletModuleManager = new DefaultServletModuleManager(new DefaultPluginEventManager()) {

            public DelegatingPluginServlet getServlet(String path, ServletConfig servletConfig) {
                return delegatingPluginServlet;
            }
        };

        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.matchAndReturn("getAttribute", C.anyArgs(1), null);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "confluence");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);
        mockHttpServletResponse.expect("sendError", C.args(C.eq(500), C.isA(String.class)));

        ServletModuleContainerServlet servlet = new ServletModuleContainerServlet() {

            protected ServletModuleManager getServletModuleManager() {
                return servletModuleManager;
            }
        };

        servlet.service((HttpServletRequest) mockHttpServletRequest.proxy(), (HttpServletResponse) mockHttpServletResponse.proxy());
    }

    private static class MockHttpServlet extends HttpServlet {

        private boolean wasCalled = false;

        @Override
        protected void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
                throws ServletException, IOException {
            wasCalled = true;
        }
    }
}
