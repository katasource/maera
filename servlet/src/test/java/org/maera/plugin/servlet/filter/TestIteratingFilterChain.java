package org.maera.plugin.servlet.filter;

import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.servlet.filter.FilterTestUtils.FilterAdapter;
import org.maera.plugin.servlet.filter.FilterTestUtils.SoundOffFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.maera.plugin.servlet.filter.FilterTestUtils.newList;
import static org.maera.plugin.servlet.filter.FilterTestUtils.singletonFilterChain;

public class TestIteratingFilterChain extends TestCase {
    public void testFiltersCalledInProperOrder() throws IOException, ServletException {
        List<Integer> filterCallOrder = new LinkedList<Integer>();
        List<Filter> filters = new ArrayList<Filter>();
        for (int i = 0; i < 5; i++) {
            filters.add(new SoundOffFilter(filterCallOrder, i));
        }

        FilterChain chain = new IteratingFilterChain(filters.iterator(), singletonFilterChain(new SoundOffFilter(filterCallOrder, 100)));

        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getPathInfo", "some/path");
        Mock mockResponse = new Mock(HttpServletResponse.class);

        chain.doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());

        // make sure that all filters were called and unrolled in the proper order
        assertEquals(newList(0, 1, 2, 3, 4, 100, 100, 4, 3, 2, 1, 0), filterCallOrder);
    }

    public void testFilterCanAbortChain() throws IOException, ServletException {
        final List<Integer> filterCallOrder = new LinkedList<Integer>();
        List<Filter> filters = new ArrayList<Filter>();
        for (int i = 0; i < 2; i++) {
            filters.add(new SoundOffFilter(filterCallOrder, i));
        }
        filters.add(new FilterAdapter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                filterCallOrder.add(50);
            }
        });
        for (int i = 3; i < 5; i++) {
            filters.add(new SoundOffFilter(filterCallOrder, i));
        }

        FilterChain chain = new IteratingFilterChain(filters.iterator(), singletonFilterChain(new SoundOffFilter(filterCallOrder, 100)));

        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getPathInfo", "some/path");
        Mock mockResponse = new Mock(HttpServletResponse.class);

        chain.doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());

        // make sure that all filters were called and unrolled in the proper order
        assertEquals(newList(0, 1, 50, 1, 0), filterCallOrder);
    }

    public void testExceptionFiltersUpWhenFilterThrowsException() throws IOException, ServletException {
        final List<Integer> filterCallOrder = new LinkedList<Integer>();
        List<Filter> filters = new ArrayList<Filter>();
        for (int i = 0; i < 2; i++) {
            filters.add(new SoundOffFilter(filterCallOrder, i));
        }
        filters.add(new FilterAdapter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                throw new ServletException();
            }
        });
        for (int i = 3; i < 5; i++) {
            filters.add(new SoundOffFilter(filterCallOrder, i));
        }

        FilterChain chain = new IteratingFilterChain(filters.iterator(), singletonFilterChain(new SoundOffFilter(filterCallOrder, 100)));

        Mock mockRequest = new Mock(HttpServletRequest.class);
        mockRequest.expectAndReturn("getPathInfo", "some/path");
        Mock mockResponse = new Mock(HttpServletResponse.class);

        try {
            chain.doFilter((HttpServletRequest) mockRequest.proxy(), (HttpServletResponse) mockResponse.proxy());
            fail("ServletException should filter up");
        }
        catch (ServletException e) {
            // yay! make sure the filter call order is as we expect
            assertEquals(newList(0, 1), filterCallOrder);
        }
    }
}
