package org.maera.plugin.servlet.filter;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class FilterTestUtils {
    public static class FilterAdapter implements Filter {
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        }

        public void destroy() {
        }
    }

    public final static class SoundOffFilter extends FilterAdapter {
        private final List<Integer> filterCallOrder;
        private final int filterId;

        public SoundOffFilter(List<Integer> filterCallOrder, int filterId) {
            this.filterCallOrder = filterCallOrder;
            this.filterId = filterId;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            filterCallOrder.add(filterId);
            chain.doFilter(request, response);
            filterCallOrder.add(filterId);
        }
    }

    public static final FilterChain emptyChain = new FilterChain() {
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        }
    };

    static <T> List<T> newList(T first, T... rest) {
        List<T> list = new LinkedList<T>();
        list.add(first);
        for (T element : rest) {
            list.add(element);
        }
        return list;
    }

    /**
     * Creates a filter chain from the single filter.  When this filter is called once, the filter chain is finished.
     */
    static FilterChain singletonFilterChain(final Filter filter) {
        return new FilterChain() {
            boolean called = false;

            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                if (!called) {
                    called = true;
                    filter.doFilter(request, response, this);
                }
            }
        };
    }

    public static <T> List<T> immutableList(List<T> list) {
        List<T> copy = new ArrayList<T>(list.size());
        copy.addAll(list);
        return Collections.unmodifiableList(copy);
    }
}
