package org.maera.plugin.servlet.filter;

import javax.servlet.*;
import java.io.IOException;
import java.util.Iterator;

/**
 * This FilterChain passes control from the first Filter in an iterator to the last.  When the last iterator
 * calls the chain.doFilter(request, response) method, the supplied "parent" chain has control returned to it.
 *
 * @since 2.1.0
 */
public final class IteratingFilterChain implements FilterChain {
    private final Iterator<Filter> iterator;
    private final FilterChain chain;

    /**
     * Create a new IteratingFilterChain which iterates over the Filters in the supplied Iterator and then returns
     * control to the main FilterChain.
     *
     * @param iterator Iterator over the Filters to apply
     * @param chain    FilterChain to return control to after the last Filter in the iterator has called the
     *                 chain.doFilter(request, response) method.
     */
    public IteratingFilterChain(Iterator<Filter> iterator, FilterChain chain) {
        this.iterator = iterator;
        this.chain = chain;
    }

    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (iterator.hasNext()) {
            Filter filter = iterator.next();
            filter.doFilter(request, response, this);
        } else {
            chain.doFilter(request, response);
        }
    }
}