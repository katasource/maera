package org.maera.plugin.servlet;

import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.classloader.PluginClassLoader;
import org.maera.plugin.impl.DefaultDynamicPlugin;
import org.maera.plugin.servlet.descriptors.ServletModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletModuleDescriptorBuilder;
import org.maera.plugin.test.PluginTestUtils;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestDelegatingPluginServlet extends TestCase {
    private PluginClassLoader classLoader;
    private Plugin plugin;
    private Mock mockRequest;
    private Mock mockResponse;

    public void setUp() throws Exception {
        classLoader = new PluginClassLoader(PluginTestUtils.getFileForResource(PluginTestUtils.SIMPLE_TEST_JAR));
        plugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), classLoader);

        mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getPathInfo", "/servlet/test");
        mockResponse = new Mock(HttpServletResponse.class);
    }

    /**
     * Test to make sure the plugin class loader is set for the thread context class loader when init is called.
     *
     * @throws Exception on test error
     */
    public void testInitCalledWithPluginClassLoaderAsThreadClassLoader() throws Exception {
        HttpServlet wrappedServlet = new HttpServlet() {
            public void init(ServletConfig config) {
                assertSame(classLoader, Thread.currentThread().getContextClassLoader());
            }
        };

        getDelegatingServlet(wrappedServlet).init(null);
    }

    /**
     * Test to make sure the plugin class loader is set for the thread context class loader when service is called.
     *
     * @throws Exception on test error
     */
    public void testServiceCalledWithPluginClassLoaderAsThreadClassLoader() throws Exception {
        HttpServlet wrappedServlet = new HttpServlet() {
            public void service(HttpServletRequest request, HttpServletResponse response) {
                assertSame(classLoader, Thread.currentThread().getContextClassLoader());
            }
        };

        getDelegatingServlet(wrappedServlet).service((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }

    /**
     * Test to make sure the servlet is called with our request wrapper.
     *
     * @throws Exception on test error
     */
    public void testServiceCalledWithWrappedRequest() throws Exception {
        HttpServlet wrappedServlet = new HttpServlet() {
            public void service(HttpServletRequest request, HttpServletResponse response) {
                assertTrue(request instanceof PluginHttpRequestWrapper);
            }
        };

        getDelegatingServlet(wrappedServlet).service((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }

    private DelegatingPluginServlet getDelegatingServlet(HttpServlet wrappedServlet) {
        ServletModuleDescriptor descriptor = new ServletModuleDescriptorBuilder()
                .with(plugin)
                .with(wrappedServlet)
                .withPath("/servlet/*")
                .build();
        return new DelegatingPluginServlet(descriptor);
    }
}
