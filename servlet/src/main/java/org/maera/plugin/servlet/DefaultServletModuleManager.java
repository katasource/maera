package org.maera.plugin.servlet;

import com.atlassian.util.concurrent.LazyReference;
import org.apache.commons.lang.Validate;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.event.PluginEventListener;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginDisabledEvent;
import org.maera.plugin.servlet.descriptors.ServletContextListenerModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletContextParamModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletFilterModuleDescriptor;
import org.maera.plugin.servlet.descriptors.ServletModuleDescriptor;
import org.maera.plugin.servlet.filter.DelegatingPluginFilter;
import org.maera.plugin.servlet.filter.FilterDispatcherCondition;
import org.maera.plugin.servlet.filter.FilterLocation;
import org.maera.plugin.servlet.filter.PluginFilterConfig;
import org.maera.plugin.servlet.util.DefaultPathMapper;
import org.maera.plugin.servlet.util.PathMapper;
import org.maera.plugin.servlet.util.ServletContextServletModuleManagerAccessor;
import org.maera.plugin.util.ClassLoaderStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.maera.plugin.servlet.descriptors.ServletFilterModuleDescriptor.byWeight;

/**
 * A simple servletModuleManager to track and retrieve the loaded servlet plugin
 * modules.
 *
 * @since 2.1.0
 */
public class DefaultServletModuleManager implements ServletModuleManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultServletModuleManager.class);

    private final PathMapper servletMapper;
    private final Map<String, ServletModuleDescriptor> servletDescriptors = new HashMap<String, ServletModuleDescriptor>();
    private final ConcurrentMap<String, LazyReference<HttpServlet>> servletRefs = new ConcurrentHashMap<String, LazyReference<HttpServlet>>();

    private final PathMapper filterMapper;
    private final Map<String, ServletFilterModuleDescriptor> filterDescriptors = new HashMap<String, ServletFilterModuleDescriptor>();
    private final ConcurrentMap<String, LazyReference<Filter>> filterRefs = new ConcurrentHashMap<String, LazyReference<Filter>>();

    private final ConcurrentMap<Plugin, ContextLifecycleReference> pluginContextRefs = new ConcurrentHashMap<Plugin, ContextLifecycleReference>();

    /**
     * Constructor that sets itself in the servlet context for later use in
     * dispatching servlets and filters.
     *
     * @param servletContext     The servlet context to store itself in
     * @param pluginEventManager The plugin event manager
     * @since 2.2.0
     */
    public DefaultServletModuleManager(final ServletContext servletContext, final PluginEventManager pluginEventManager) {
        this(pluginEventManager);
        ServletContextServletModuleManagerAccessor.setServletModuleManager(servletContext, this);
    }

    /**
     * Creates the servlet module manager, but assumes you will be calling
     * {@link org.maera.plugin.servlet.util.ServletContextServletModuleManagerAccessor#setServletModuleManager(javax.servlet.ServletContext, ServletModuleManager)}
     * yourself if you don't extend the dispatching servlet and filter classes
     * to provide the servlet module manager instance.
     *
     * @param pluginEventManager The plugin event manager
     */
    public DefaultServletModuleManager(final PluginEventManager pluginEventManager) {
        this(pluginEventManager, new DefaultPathMapper(), new DefaultPathMapper());
    }

    /**
     * Creates the servlet module manager, but assumes you will be calling
     * {@link org.maera.plugin.servlet.util.ServletContextServletModuleManagerAccessor#setServletModuleManager(javax.servlet.ServletContext, ServletModuleManager)}
     * yourself if you don't extend the dispatching servlet and filter classes
     * to provide the servlet module manager instance.
     *
     * @param pluginEventManager The plugin event manager
     * @param servletPathMapper  The path mapper used for mapping servlets to
     *                           paths
     * @param filterPathMapper   The path mapper used for mapping filters to paths
     */
    public DefaultServletModuleManager(final PluginEventManager pluginEventManager, final PathMapper servletPathMapper, final PathMapper filterPathMapper) {
        servletMapper = servletPathMapper;
        filterMapper = filterPathMapper;
        pluginEventManager.register(this);
    }

    public void addServletModule(final ServletModuleDescriptor descriptor) {
        servletDescriptors.put(descriptor.getCompleteKey(), descriptor);

        // for some reason the JDK complains about getPaths not returning a
        // List<String> ?!?!?
        final List<String> paths = descriptor.getPaths();
        for (final String path : paths) {
            servletMapper.put(descriptor.getCompleteKey(), path);
        }
        final LazyReference<HttpServlet> servletRef = servletRefs.remove(descriptor.getCompleteKey());
        if (servletRef != null) {
            servletRef.get().destroy();
        }
    }

    public HttpServlet getServlet(final String path, final ServletConfig servletConfig) throws ServletException {
        final String completeKey = servletMapper.get(path);

        if (completeKey == null) {
            return null;
        }
        final ServletModuleDescriptor descriptor = servletDescriptors.get(completeKey);
        if (descriptor == null) {
            return null;
        }

        final HttpServlet servlet = getServlet(descriptor, servletConfig);
        if (servlet == null) {
            servletRefs.remove(descriptor.getCompleteKey());
        }
        return servlet;
    }

    public void removeServletModule(final ServletModuleDescriptor descriptor) {
        servletDescriptors.remove(descriptor.getCompleteKey());
        servletMapper.put(descriptor.getCompleteKey(), null);

        final LazyReference<HttpServlet> servletRef = servletRefs.remove(descriptor.getCompleteKey());
        if (servletRef != null) {
            servletRef.get().destroy();
        }
    }

    public void addFilterModule(final ServletFilterModuleDescriptor descriptor) {
        filterDescriptors.put(descriptor.getCompleteKey(), descriptor);

        for (final String path : descriptor.getPaths()) {
            filterMapper.put(descriptor.getCompleteKey(), path);
        }
        final LazyReference<Filter> filterRef = filterRefs.remove(descriptor.getCompleteKey());
        if (filterRef != null) {
            filterRef.get().destroy();
        }
    }

    public Iterable<Filter> getFilters(final FilterLocation location, final String path, final FilterConfig filterConfig) throws ServletException {
        return getFilters(location, path, filterConfig, FilterDispatcherCondition.REQUEST);
    }

    public Iterable<Filter> getFilters(FilterLocation location, String path, FilterConfig filterConfig, FilterDispatcherCondition condition) throws ServletException {
        Validate.notNull(condition);
        final List<ServletFilterModuleDescriptor> matchingFilterDescriptors = new ArrayList<ServletFilterModuleDescriptor>();

        for (final String completeKey : filterMapper.getAll(path)) {
            final ServletFilterModuleDescriptor descriptor = filterDescriptors.get(completeKey);
            if (!descriptor.getDispatcherConditions().contains(condition)) {
                if (log.isTraceEnabled()) {
                    log.trace("Skipping filter " + descriptor.getCompleteKey() + " as condition " + condition +
                            " doesn't match list:" + Arrays.asList(descriptor.getDispatcherConditions()));
                }
                continue;
            }

            if (location.equals(descriptor.getLocation())) {
                sortedInsert(matchingFilterDescriptors, descriptor, byWeight);
            }
        }
        final List<Filter> filters = new LinkedList<Filter>();
        for (final ServletFilterModuleDescriptor descriptor : matchingFilterDescriptors) {
            final Filter filter = getFilter(descriptor, filterConfig);
            if (filter == null) {
                filterRefs.remove(descriptor.getCompleteKey());
            } else {
                filters.add(getFilter(descriptor, filterConfig));
            }
        }

        return filters;
    }

    static <T> void sortedInsert(final List<T> list, final T e, final Comparator<T> comparator) {
        int insertIndex = Collections.binarySearch(list, e, comparator);
        if (insertIndex < 0) {
            // no entry already there, so the insertIndex is the negative value
            // of where it should be inserted
            insertIndex = -insertIndex - 1;
        } else {
            // there is already a value at that position, so we need to find the
            // next available spot for it
            while ((insertIndex < list.size()) && (comparator.compare(list.get(insertIndex), e) == 0)) {
                insertIndex++;
            }
        }
        list.add(insertIndex, e);
    }

    public void removeFilterModule(final ServletFilterModuleDescriptor descriptor) {
        filterDescriptors.remove(descriptor.getCompleteKey());
        filterMapper.put(descriptor.getCompleteKey(), null);

        final LazyReference<Filter> filterRef = filterRefs.remove(descriptor.getCompleteKey());
        if (filterRef != null) {
            filterRef.get().destroy();
        }
    }

    /**
     * Call the plugins servlet context listeners contextDestroyed methods and
     * cleanup any servlet contexts that are associated with the plugin that was
     * disabled.
     */
    @PluginEventListener
    public void onPluginDisabled(final PluginDisabledEvent event) {
        final Plugin plugin = event.getPlugin();
        final ContextLifecycleReference context = pluginContextRefs.remove(plugin);
        if (context == null) {
            return;
        }

        context.get().contextDestroyed();
    }

    /**
     * Returns a wrapped Servlet for the servlet module. If a wrapped servlet
     * for the module has not been created yet, we create one using the
     * servletConfig.
     * <p/>
     * Note: We use a map of lazily loaded references to the servlet so that
     * only one can ever be created and initialized for each module descriptor.
     *
     * @param descriptor
     * @param servletConfig
     * @return
     */
    HttpServlet getServlet(final ServletModuleDescriptor descriptor, final ServletConfig servletConfig) {
        // check for an existing reference, if there is one it's either in the
        // process of loading, in which case
        // servletRef.get() below will block until it's available, otherwise we
        // go about creating a new ref to use
        LazyReference<HttpServlet> servletRef = servletRefs.get(descriptor.getCompleteKey());
        if (servletRef == null) {
            // if there isn't an existing reference, create one.
            final ServletContext servletContext = getWrappedContext(descriptor.getPlugin(), servletConfig.getServletContext());
            servletRef = new LazyLoadedServletReference(descriptor, servletContext);

            // check that another thread didn't beat us to the punch of creating
            // a lazy reference. if it did, we
            // want to use that so there is only ever one reference
            if (servletRefs.putIfAbsent(descriptor.getCompleteKey(), servletRef) != null) {
                servletRef = servletRefs.get(descriptor.getCompleteKey());
            }
        }
        HttpServlet servlet = null;
        try {
            servlet = servletRef.get();
        }
        catch (final RuntimeException ex) {
            log.error("Unable to create servlet", ex);
        }
        return servlet;
    }

    /**
     * Returns a wrapped Filter for the filter module. If a wrapped filter for
     * the module has not been created yet, we create one using the
     * filterConfig.
     * <p/>
     * Note: We use a map of lazily loaded references to the filter so that only
     * one can ever be created and initialized for each module descriptor.
     *
     * @param descriptor
     * @param filterConfig
     * @return The filter, or null if the filter is invalid and should be
     *         removed
     */
    Filter getFilter(final ServletFilterModuleDescriptor descriptor, final FilterConfig filterConfig) {
        // check for an existing reference, if there is one it's either in the
        // process of loading, in which case
        // filterRef.get() below will block until it's available, otherwise we
        // go about creating a new ref to use
        LazyReference<Filter> filterRef = filterRefs.get(descriptor.getCompleteKey());
        if (filterRef == null) {
            // if there isn't an existing reference, create one.
            final ServletContext servletContext = getWrappedContext(descriptor.getPlugin(), filterConfig.getServletContext());
            filterRef = new LazyLoadedFilterReference(descriptor, servletContext);

            // check that another thread didn't beat us to the punch of creating
            // a lazy reference. if it did, we
            // want to use that so there is only ever one reference
            if (filterRefs.putIfAbsent(descriptor.getCompleteKey(), filterRef) != null) {
                filterRef = filterRefs.get(descriptor.getCompleteKey());
            }
        }
        try {
            return filterRef.get();
        }
        catch (final RuntimeException ex) {
            log.error("Unable to create filter", ex);
            return null;
        }
    }

    /**
     * Returns a wrapped ServletContext for the plugin. If a wrapped servlet
     * context for the plugin has not been created yet, we create using the
     * baseContext, any context params specified in the plugin and initialize
     * any context listeners the plugin may define.
     * <p/>
     * Note: We use a map of lazily loaded references to the context so that
     * only one can ever be created for each plugin.
     *
     * @param plugin      Plugin for whom we're creating a wrapped servlet context.
     * @param baseContext The applications base servlet context which we will be
     *                    wrapping.
     * @return A wrapped, fully initialized servlet context that can be used for
     *         all the plugins filters and servlets.
     */
    private ServletContext getWrappedContext(final Plugin plugin, final ServletContext baseContext) {
        ContextLifecycleReference pluginContextRef = pluginContextRefs.get(plugin);
        if (pluginContextRef == null) {
            pluginContextRef = new ContextLifecycleReference(plugin, baseContext);
            if (pluginContextRefs.putIfAbsent(plugin, pluginContextRef) != null) {
                pluginContextRef = pluginContextRefs.get(plugin);
            }
        }
        return pluginContextRef.get().servletContext;
    }

    private static final class LazyLoadedFilterReference extends LazyReference<Filter> {
        private final ServletFilterModuleDescriptor descriptor;
        private final ServletContext servletContext;

        private LazyLoadedFilterReference(final ServletFilterModuleDescriptor descriptor, final ServletContext servletContext) {
            this.descriptor = descriptor;
            this.servletContext = servletContext;
        }

        @Override
        protected Filter create() throws Exception {
            final Filter filter = new DelegatingPluginFilter(descriptor);
            filter.init(new PluginFilterConfig(descriptor, servletContext));
            return filter;
        }
    }

    private static final class LazyLoadedServletReference extends LazyReference<HttpServlet> {
        private final ServletModuleDescriptor descriptor;
        private final ServletContext servletContext;

        private LazyLoadedServletReference(final ServletModuleDescriptor descriptor, final ServletContext servletContext) {
            this.descriptor = descriptor;
            this.servletContext = servletContext;
        }

        @Override
        protected HttpServlet create() throws Exception {
            final HttpServlet servlet = new DelegatingPluginServlet(descriptor);
            servlet.init(new PluginServletConfig(descriptor, servletContext));
            return servlet;
        }
    }

    private static final class ContextLifecycleReference extends LazyReference<ContextLifecycleManager> {
        private final Plugin plugin;
        private final ServletContext baseContext;

        private ContextLifecycleReference(final Plugin plugin, final ServletContext baseContext) {
            this.plugin = plugin;
            this.baseContext = baseContext;
        }

        @Override
        protected ContextLifecycleManager create() throws Exception {
            final ConcurrentMap<String, Object> contextAttributes = new ConcurrentHashMap<String, Object>();
            final Map<String, String> initParams = mergeInitParams(baseContext, plugin);
            final ServletContext context = new PluginServletContextWrapper(plugin, baseContext, contextAttributes, initParams);

            ClassLoaderStack.push(plugin.getClassLoader());
            final List<ServletContextListener> listeners = new ArrayList<ServletContextListener>();
            try {
                for (final ServletContextListenerModuleDescriptor descriptor : findModuleDescriptorsByType(ServletContextListenerModuleDescriptor.class, plugin)) {
                    listeners.add(descriptor.getModule());
                }
            }
            finally {
                ClassLoaderStack.pop();
            }

            return new ContextLifecycleManager(context, listeners);
        }

        private Map<String, String> mergeInitParams(final ServletContext baseContext, final Plugin plugin) {
            final Map<String, String> mergedInitParams = new HashMap<String, String>();
            @SuppressWarnings("unchecked")
            final Enumeration<String> e = baseContext.getInitParameterNames();
            while (e.hasMoreElements()) {
                final String paramName = e.nextElement();
                mergedInitParams.put(paramName, baseContext.getInitParameter(paramName));
            }
            for (final ServletContextParamModuleDescriptor descriptor : findModuleDescriptorsByType(ServletContextParamModuleDescriptor.class, plugin)) {
                mergedInitParams.put(descriptor.getParamName(), descriptor.getParamValue());
            }
            return Collections.unmodifiableMap(mergedInitParams);
        }
    }

    static <T extends ModuleDescriptor<?>> Iterable<T> findModuleDescriptorsByType(final Class<T> type, final Plugin plugin) {
        final Set<T> descriptors = new HashSet<T>();
        for (final ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors()) {
            if (type.isAssignableFrom(descriptor.getClass())) {
                descriptors.add(type.cast(descriptor));
            }
        }
        return descriptors;
    }

    static final class ContextLifecycleManager {
        private final ServletContext servletContext;
        private final Iterable<ServletContextListener> listeners;

        ContextLifecycleManager(final ServletContext servletContext, final Iterable<ServletContextListener> listeners) {
            this.servletContext = servletContext;
            this.listeners = listeners;
            for (final ServletContextListener listener : listeners) {
                listener.contextInitialized(new ServletContextEvent(servletContext));
            }
        }

        ServletContext getServletContext() {
            return servletContext;
        }

        void contextDestroyed() {
            final ServletContextEvent event = new ServletContextEvent(servletContext);
            for (final ServletContextListener listener : listeners) {
                listener.contextDestroyed(event);
            }
        }
    }
}
