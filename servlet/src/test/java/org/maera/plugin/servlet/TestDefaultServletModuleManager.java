package org.maera.plugin.servlet;

import com.google.common.collect.Iterables;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.Plugin;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.servlet.descriptors.*;
import org.maera.plugin.servlet.filter.DelegatingPluginFilter;
import org.maera.plugin.servlet.filter.FilterLocation;
import org.maera.plugin.servlet.filter.FilterTestUtils.FilterAdapter;
import org.maera.plugin.servlet.filter.FilterTestUtils.SoundOffFilter;
import org.maera.plugin.servlet.filter.IteratingFilterChain;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.maera.plugin.servlet.DefaultServletModuleManager.sortedInsert;
import static org.maera.plugin.servlet.filter.FilterDispatcherCondition.*;
import static org.maera.plugin.servlet.filter.FilterTestUtils.emptyChain;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestDefaultServletModuleManager extends TestCase {
    ServletModuleManager servletModuleManager;

    Mock mockPluginEventManager;

    public void setUp() {
        mockPluginEventManager = new Mock(PluginEventManager.class);
        mockPluginEventManager.expect("register", C.anyArgs(1));
        servletModuleManager = new DefaultServletModuleManager((PluginEventManager) mockPluginEventManager.proxy());
    }

    public void testSortedInsertInsertsDistinctElementProperly() {
        List<String> list = newList("cat", "dog", "fish", "monkey");
        List<String> endList = newList("cat", "dog", "elephant", "fish", "monkey");
        sortedInsert(list, "elephant", naturalOrder(String.class));
        assertEquals(endList, list);
    }

    public void testSortedInsertInsertsNonDistinctElementProperly() {
        List<WeightedValue> list = newList
                (
                        new WeightedValue(10, "dog"), new WeightedValue(20, "monkey"), new WeightedValue(20, "tiger"),
                        new WeightedValue(30, "fish"), new WeightedValue(100, "cat")
                );
        List<WeightedValue> endList = newList
                (
                        new WeightedValue(10, "dog"), new WeightedValue(20, "monkey"), new WeightedValue(20, "tiger"),
                        new WeightedValue(20, "elephant"), new WeightedValue(30, "fish"), new WeightedValue(100, "cat")
                );
        sortedInsert(list, new WeightedValue(20, "elephant"), WeightedValue.byWeight);
        assertEquals(endList, list);
    }

    public void testGettingServletWithSimplePath() throws Exception {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());

        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "/servlet");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);

        TestHttpServlet servlet = new TestHttpServlet();
        ServletModuleDescriptor descriptor = new ServletModuleDescriptorBuilder()
                .with(servlet)
                .withPath("/servlet")
                .with(servletModuleManager)
                .build();

        servletModuleManager.addServletModule(descriptor);

        HttpServlet wrappedServlet = servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy());
        wrappedServlet.service((HttpServletRequest) mockHttpServletRequest.proxy(), (HttpServletResponse) mockHttpServletResponse.proxy());
        assertTrue(servlet.serviceCalled);
    }

    public void testGettingServlet() {
        getServletTwice(false);
    }

    private void getServletTwice(boolean expectNewServletEachCall) {
        mockPluginEventManager.expect("register", C.anyArgs(1));
        DefaultServletModuleManager mgr = new DefaultServletModuleManager((PluginEventManager) mockPluginEventManager.proxy());

        AtomicReference<HttpServlet> servletRef = new AtomicReference<HttpServlet>();
        TestHttpServlet firstServlet = new TestHttpServlet();
        servletRef.set(firstServlet);
        ServletModuleDescriptor descriptor = new ServletModuleDescriptorBuilder()
                .withFactory(ObjectFactories.createMutable(servletRef))
                .withPath("/servlet")
                .with(mgr)
                .build();

        final ServletConfig servletConfig = mock(ServletConfig.class);
        final ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getInitParameterNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
        when(servletConfig.getServletContext()).thenReturn(servletContext);

        assertTrue(firstServlet == ((DelegatingPluginServlet) mgr.getServlet(descriptor, servletConfig)).getDelegatingServlet());

        TestHttpServlet secondServlet = new TestHttpServlet();
        servletRef.set(secondServlet);
        HttpServlet expectedServlet = (expectNewServletEachCall ? secondServlet : firstServlet);
        assertTrue(expectedServlet == ((DelegatingPluginServlet) mgr.getServlet(descriptor, servletConfig)).getDelegatingServlet());
    }

    public void testGettingFilter() {
        getFilterTwice(false);
    }

    private void getFilterTwice(boolean expectNewFilterEachCall) {
        mockPluginEventManager.expect("register", C.anyArgs(1));
        DefaultServletModuleManager mgr = new DefaultServletModuleManager((PluginEventManager) mockPluginEventManager.proxy());

        AtomicReference<Filter> filterRef = new AtomicReference<Filter>();
        TestHttpFilter firstFilter = new TestHttpFilter();
        filterRef.set(firstFilter);
        ServletFilterModuleDescriptor descriptor = new ServletFilterModuleDescriptorBuilder()
                .withFactory(ObjectFactories.createMutable(filterRef))
                .withPath("/servlet")
                .with(mgr)
                .build();

        final FilterConfig filterConfig = mock(FilterConfig.class);
        final ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getInitParameterNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
        when(filterConfig.getServletContext()).thenReturn(servletContext);

        assertTrue(firstFilter == ((DelegatingPluginFilter) mgr.getFilter(descriptor, filterConfig)).getDelegatingFilter());

        TestHttpFilter secondFilter = new TestHttpFilter();
        filterRef.set(secondFilter);
        Filter expectedFilter = (expectNewFilterEachCall ? secondFilter : firstFilter);
        assertTrue(expectedFilter == ((DelegatingPluginFilter) mgr.getFilter(descriptor, filterConfig)).getDelegatingFilter());
    }

    public void testGettingServletWithException() throws Exception {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());

        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "/servlet");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);

        TestHttpServletWithException servlet = new TestHttpServletWithException();
        ServletModuleDescriptor descriptor = new ServletModuleDescriptorBuilder()
                .with(servlet)
                .withPath("/servlet")
                .with(servletModuleManager)
                .build();

        servletModuleManager.addServletModule(descriptor);

        assertNull(servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy()));
    }

    public void testGettingFilterWithException() throws Exception {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockFilterConfig = new Mock(FilterConfig.class);
        mockFilterConfig.expectAndReturn("getServletContext", mockServletContext.proxy());

        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "/servlet");

        TestFilterWithException servlet = new TestFilterWithException();
        ServletFilterModuleDescriptor descriptor = new ServletFilterModuleDescriptorBuilder()
                .with(servlet)
                .withPath("/servlet")
                .with(servletModuleManager)
                .at(FilterLocation.AFTER_ENCODING)
                .build();

        servletModuleManager.addFilterModule(descriptor);

        assertEquals(false, servletModuleManager.getFilters(FilterLocation.AFTER_ENCODING, "/servlet", (FilterConfig) mockFilterConfig.proxy()).iterator().hasNext());
    }

    public void testGettingServletWithComplexPath() throws Exception {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());

        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.expectAndReturn("getPathInfo", "/servlet");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);

        TestHttpServlet servlet = new TestHttpServlet();
        ServletModuleDescriptor descriptor = new ServletModuleDescriptorBuilder()
                .with(servlet)
                .withPath("/servlet/*")
                .with(servletModuleManager)
                .build();

        servletModuleManager.addServletModule(descriptor);

        HttpServlet wrappedServlet = servletModuleManager.getServlet("/servlet/this/is/a/test", (ServletConfig) mockServletConfig.proxy());
        wrappedServlet.service((HttpServletRequest) mockHttpServletRequest.proxy(), (HttpServletResponse) mockHttpServletResponse.proxy());
        assertTrue(servlet.serviceCalled);
    }

    public void testMultipleFitlersWithTheSameComplexPath() throws ServletException {
        ServletContext servletContext = mock(ServletContext.class);
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getInitParameterNames()).thenReturn(new Vector().elements());
        Plugin plugin = new PluginBuilder().build();
        ServletFilterModuleDescriptor filterDescriptor = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .withKey("foo")
                .with(new FilterAdapter())
                .withPath("/foo/*")
                .with(servletModuleManager)
                .build();

        ServletFilterModuleDescriptor filterDescriptor2 = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .withKey("bar")
                .with(new FilterAdapter())
                .withPath("/foo/*")
                .with(servletModuleManager)
                .build();
        servletModuleManager.addFilterModule(filterDescriptor);
        servletModuleManager.addFilterModule(filterDescriptor2);

        servletModuleManager.removeFilterModule(filterDescriptor);
        assertTrue(servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/foo/jim", filterConfig).iterator().hasNext());
    }

    public void testMultipleFitlersWithTheSameSimplePath() throws ServletException {
        ServletContext servletContext = mock(ServletContext.class);
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getInitParameterNames()).thenReturn(new Vector().elements());
        Plugin plugin = new PluginBuilder().build();
        ServletFilterModuleDescriptor filterDescriptor = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .withKey("foo")
                .with(new FilterAdapter())
                .withPath("/foo")
                .with(servletModuleManager)
                .build();

        ServletFilterModuleDescriptor filterDescriptor2 = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .withKey("bar")
                .with(new FilterAdapter())
                .withPath("/foo")
                .with(servletModuleManager)
                .build();
        servletModuleManager.addFilterModule(filterDescriptor);
        servletModuleManager.addFilterModule(filterDescriptor2);

        servletModuleManager.removeFilterModule(filterDescriptor);
        assertTrue(servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/foo", filterConfig).iterator().hasNext());
    }

    public void testPluginContextInitParamsGetMerged() throws Exception {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());

        Plugin plugin = new PluginBuilder().build();

        new ServletContextParamDescriptorBuilder()
                .with(plugin)
                .withParam("param.name", "param.value")
                .build();

        // a servlet that will check for param.name to be in the servlet context
        ServletModuleDescriptor servletDescriptor = new ServletModuleDescriptorBuilder()
                .with(plugin)
                .with(new TestHttpServlet() {
                    @Override
                    public void init(ServletConfig servletConfig) {
                        assertEquals("param.value", servletConfig.getServletContext().getInitParameter("param.name"));
                    }
                })
                .withPath("/servlet")
                .with(servletModuleManager)
                .build();
        servletModuleManager.addServletModule(servletDescriptor);

        servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy());
    }

    public void testServletListenerContextInitializedIsCalled() throws Exception {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());

        final TestServletContextListener listener = new TestServletContextListener();

        Plugin plugin = new PluginBuilder().build();

        new ServletContextListenerModuleDescriptorBuilder()
                .with(plugin)
                .with(listener)
                .build();

        ServletModuleDescriptor servletDescriptor = new ServletModuleDescriptorBuilder()
                .with(plugin)
                .with(new TestHttpServlet())
                .withPath("/servlet")
                .with(servletModuleManager)
                .build();

        servletModuleManager.addServletModule(servletDescriptor);
        servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy());
        assertTrue(listener.initCalled);
    }

    public void testServletListenerContextFilterAndServletUseTheSameServletContext() throws Exception {
        Plugin plugin = new PluginBuilder().build();

        final AtomicReference<ServletContext> contextRef = new AtomicReference<ServletContext>();
        // setup a context listener to capture the context
        new ServletContextListenerModuleDescriptorBuilder()
                .with(plugin)
                .with(new TestServletContextListener() {
                    @Override
                    public void contextInitialized(ServletContextEvent event) {
                        contextRef.set(event.getServletContext());
                    }
                })
                .build();

        // a servlet that checks that the context is the same for it as it was for the context listener
        ServletModuleDescriptor servletDescriptor = new ServletModuleDescriptorBuilder()
                .with(plugin)
                .with(new TestHttpServlet() {
                    @Override
                    public void init(ServletConfig servletConfig) {
                        assertSame(contextRef.get(), servletConfig.getServletContext());
                    }
                })
                .withPath("/servlet")
                .with(servletModuleManager)
                .build();
        servletModuleManager.addServletModule(servletDescriptor);

        // a filter that checks that the context is the same for it as it was for the context listener
        ServletFilterModuleDescriptor filterDescriptor = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .with(new FilterAdapter() {
                    @Override
                    public void init(FilterConfig filterConfig) {
                        assertSame(contextRef.get(), filterConfig.getServletContext());
                    }
                })
                .withPath("/*")
                .with(servletModuleManager)
                .build();
        servletModuleManager.addFilterModule(filterDescriptor);

        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.expectAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);

        // get a servlet, this will initialize the servlet context for the first time in addition to the servlet itself.
        // if the servlet doesn't get the same context as the context listener did, the assert will fail
        Mock mockServletConfig = new Mock(ServletConfig.class);
        mockServletConfig.expectAndReturn("getServletContext", mockServletContext.proxy());
        servletModuleManager.getServlet("/servlet", (ServletConfig) mockServletConfig.proxy());

        // get the filters, if the filter doesn't get the same context as the context listener did, the assert will fail
        Mock mockFilterConfig = new Mock(FilterConfig.class);
        mockFilterConfig.expectAndReturn("getServletContext", mockServletContext.proxy());
        servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/servlet", (FilterConfig) mockFilterConfig.proxy());
    }

    public void testFiltersWithSameLocationAndWeightInTheSamePluginAppearInTheOrderTheyAreDeclared() throws Exception {
        Mock mockServletContext = new Mock(ServletContext.class);
        mockServletContext.matchAndReturn("getInitParameterNames", Collections.enumeration(Collections.emptyList()));
        mockServletContext.expect("log", C.ANY_ARGS);
        Mock mockFilterConfig = new Mock(FilterConfig.class);
        mockFilterConfig.matchAndReturn("getServletContext", mockServletContext.proxy());

        Plugin plugin = new PluginBuilder().build();

        List<Integer> filterCallOrder = new LinkedList<Integer>();
        ServletFilterModuleDescriptor d1 = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .withKey("filter-1")
                .with(new SoundOffFilter(filterCallOrder, 1))
                .withPath("/*")
                .build();
        servletModuleManager.addFilterModule(d1);

        ServletFilterModuleDescriptor d2 = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .withKey("filter-2")
                .with(new SoundOffFilter(filterCallOrder, 2))
                .withPath("/*")
                .build();
        servletModuleManager.addFilterModule(d2);

        Mock mockHttpServletRequest = new Mock(HttpServletRequest.class);
        mockHttpServletRequest.matchAndReturn("getPathInfo", "/servlet");
        Mock mockHttpServletResponse = new Mock(HttpServletResponse.class);

        Iterable<Filter> filters = servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/some/path", (FilterConfig) mockFilterConfig.proxy());
        FilterChain chain = new IteratingFilterChain(filters.iterator(), emptyChain);

        chain.doFilter((HttpServletRequest) mockHttpServletRequest.proxy(), (HttpServletResponse) mockHttpServletResponse.proxy());
        assertEquals(newList(1, 2, 2, 1), filterCallOrder);
    }

    public void testGetFiltersWithDispatcher() throws Exception {
        ServletContext servletContext = mock(ServletContext.class);
        FilterConfig filterConfig = mock(FilterConfig.class);
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getInitParameterNames()).thenReturn(new Vector().elements());
        Plugin plugin = new PluginBuilder().build();

        ServletFilterModuleDescriptor filterDescriptor = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .withKey("foo")
                .with(new FilterAdapter())
                .withPath("/foo")
                .with(servletModuleManager)
                .withDispatcher(REQUEST)
                .withDispatcher(FORWARD)
                .build();

        ServletFilterModuleDescriptor filterDescriptor2 = new ServletFilterModuleDescriptorBuilder()
                .with(plugin)
                .withKey("bar")
                .with(new FilterAdapter())
                .withPath("/foo")
                .with(servletModuleManager)
                .withDispatcher(REQUEST)
                .withDispatcher(INCLUDE)
                .build();

        servletModuleManager.addFilterModule(filterDescriptor);
        servletModuleManager.addFilterModule(filterDescriptor2);

        assertEquals(2, Iterables.size(servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/foo", filterConfig, REQUEST)));
        assertEquals(1, Iterables.size(servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/foo", filterConfig, INCLUDE)));
        assertEquals(1, Iterables.size(servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/foo", filterConfig, FORWARD)));
        assertEquals(0, Iterables.size(servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/foo", filterConfig, ERROR)));

        try {
            servletModuleManager.getFilters(FilterLocation.BEFORE_DISPATCH, "/foo", filterConfig, null);
            fail("Shouldn't accept nulls");
        }
        catch (IllegalArgumentException ex) {
            // this is good
        }
    }

    static class TestServletContextListener implements ServletContextListener {
        boolean initCalled = false;

        public void contextInitialized(ServletContextEvent event) {
            initCalled = true;
        }

        public void contextDestroyed(ServletContextEvent event) {
        }
    }

    static class TestHttpServlet extends HttpServlet {
        boolean serviceCalled = false;

        @Override
        public void service(ServletRequest request, ServletResponse response) {
            serviceCalled = true;
        }
    }

    static class TestHttpServletWithException extends HttpServlet {
        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            throw new RuntimeException("exception thrown");
        }
    }

    static class TestFilterWithException implements Filter {
        public void init(FilterConfig filterConfig) throws ServletException {
            throw new RuntimeException("exception thrown");
        }

        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        }

        public void destroy() {
        }
    }

    static class TestHttpFilter implements Filter {
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        }

        public void destroy() {
        }
    }

    static final class WeightedValue {
        final int weight;
        final String value;

        WeightedValue(int weight, String value) {
            this.weight = weight;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof WeightedValue))
                return false;
            WeightedValue rhs = (WeightedValue) o;
            return weight == rhs.weight && value.equals(rhs.value);
        }

        @Override
        public String toString() {
            return "[" + weight + ", " + value + "]";
        }

        static final Comparator<WeightedValue> byWeight = new Comparator<WeightedValue>() {
            public int compare(WeightedValue o1, WeightedValue o2) {
                return Integer.valueOf(o1.weight).compareTo(o2.weight);
            }
        };
    }

    static <T> List<T> newList(T... elements) {
        List<T> list = new ArrayList<T>();
        for (T e : elements) {
            list.add(e);
        }
        return list;
    }

    static <T extends Comparable<T>> Comparator<T> naturalOrder(Class<T> type) {
        return new Comparator<T>() {
            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        };
    }
}
