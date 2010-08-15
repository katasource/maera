package org.maera.plugin.servlet.filter;

import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.classloader.PluginClassLoader;
import org.maera.plugin.impl.DefaultDynamicPlugin;
import org.maera.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletFilterModuleDescriptorBuilder;
import org.maera.plugin.servlet.filter.FilterTestUtils.FilterAdapter;
import org.maera.plugin.test.PluginJarBuilder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

import static org.maera.plugin.servlet.filter.FilterTestUtils.emptyChain;
import static org.maera.plugin.servlet.filter.FilterTestUtils.newList;
import static org.maera.plugin.test.PluginTestUtils.getFileForResource;

public class TestDelegatingPluginFilter extends TestCase {
    public void testPluginClassLoaderIsThreadContextClassLoaderWhenFiltering() throws Exception {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getPathInfo", "/servlet");

        Mock mockResponse = new Mock(HttpServletResponse.class);

        createClassLoaderCheckingFilter("filter").doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy(), emptyChain);
    }

    public void testClassLoaderResetDuringFilterChainExecution() throws Exception {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getPathInfo", "/servlet");

        Mock mockResponse = new Mock(HttpServletResponse.class);

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        FilterChain chain = new FilterChain() {
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
                assertEquals(cl, Thread.currentThread().getContextClassLoader());
            }
        };
        createClassLoaderCheckingFilter("filter").doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy(), chain);
    }

    public void testPluginClassLoaderIsThreadContextLoaderWhenFiltersInChainAreFromDifferentPlugins() throws Exception {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getPathInfo", "/servlet");
        Mock mockResponse = new Mock(HttpServletResponse.class);

        Iterable<Filter> filters = newList(
                createClassLoaderCheckingFilter("filter-1"),
                createClassLoaderCheckingFilter("filter-2"),
                createClassLoaderCheckingFilter("filter-3")
        );
        FilterChain chain = new IteratingFilterChain(filters.iterator(), emptyChain);
        chain.doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
    }

    public void testPluginClassLoaderIsRestoredProperlyWhenAnExceptionIsThrownFromFilter() throws Exception {
        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.matchAndReturn("getPathInfo", "/servlet");
        Mock mockResponse = new Mock(HttpServletResponse.class);

        Iterable<Filter> filters = newList(
                createClassLoaderCheckingFilter("filter-1"),
                createClassLoaderCheckingFilter("filter-2"),
                createExceptionThrowingFilter("exception-filter"),
                createClassLoaderCheckingFilter("filter-3")
        );
        FilterChain chain = new IteratingFilterChain(filters.iterator(), new FilterChain() {
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                fail("Exception should be thrown before reaching here.");
            }
        });
        try {
            chain.doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
            fail("Exception should have been thrown");
        }
        catch (ServletException e) {
            // yay
        }
    }

    private Filter createClassLoaderCheckingFilter(final String name) throws Exception {
        File pluginFile = new PluginJarBuilder()
                .addFormattedJava("my.SimpleFilter",
                        "package my;" +
                                "import java.io.IOException;" +
                                "import javax.servlet.Filter;" +
                                "import javax.servlet.FilterChain;" +
                                "import javax.servlet.FilterConfig;" +
                                "import javax.servlet.ServletException;" +
                                "import javax.servlet.ServletRequest;" +
                                "import javax.servlet.ServletResponse;" +
                                "" +
                                "public class SimpleFilter implements Filter" +
                                "{" +
                                "    String name;" +
                                "    public void init(FilterConfig filterConfig) throws ServletException" +
                                "    {" +
                                "        name = filterConfig.getInitParameter('name');" +
                                "    }" +
                                "" +
                                "    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException" +
                                "    {" +
                                "        response.getWriter().write('entered: ' + name + '\');" +
                                "        chain.doFilter(request, response);" +
                                "        response.getWriter().write('exiting: ' + name + '\');" +
                                "    }" +
                                "    public void destroy() {}" +
                                "}")
                .addFile("maera-plugin.xml", getFileForResource("org/maera/plugin/servlet/filter/maera-plugin-filter.xml"))
                .build();
        final PluginClassLoader loader = new PluginClassLoader(pluginFile);
        Plugin plugin = new DefaultDynamicPlugin((PluginArtifact) new Mock(PluginArtifact.class).proxy(), loader);
        FilterAdapter testFilter = new FilterAdapter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                assertSame(name + " loader should be the thread context ClassLoader when entering", loader, Thread.currentThread().getContextClassLoader());
                chain.doFilter(request, response);
                assertSame(name + " loader should be the thread context ClassLoader when exiting", loader, Thread.currentThread().getContextClassLoader());
            }
        };

        ServletFilterModuleDescriptor filterDescriptor = new ServletFilterModuleDescriptorBuilder()
                .with(testFilter)
                .with(plugin)
                .build();

        final Filter delegatingFilter = new DelegatingPluginFilter(filterDescriptor);
        return delegatingFilter;
    }

    private Filter createExceptionThrowingFilter(final String name) {
        return new FilterAdapter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                throw new ServletException(name);
            }
        };
    }
}
