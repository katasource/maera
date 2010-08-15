package org.maera.plugin.webresource;

import org.maera.plugin.ModuleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

/**
 * A handy super-class that handles most of the resource management.
 * <p/>
 * To use this manager, you need to have the following UrlRewriteFilter code:
 * <pre>
 * &lt;rule>
 * &lt;from>^/s/(.*)/_/(.*)&lt;/from>
 * &lt;run class="org.maera.plugin.servlet.ResourceDownloadUtils" method="addCachingHeaders" />
 * &lt;to type="forward">/$2&lt;/to>
 * &lt;/rule>
 * </pre>
 * <p/>
 * Sub-classes should implement the abstract methods
 */
public class WebResourceManagerImpl implements WebResourceManager {
    private static final Logger log = LoggerFactory.getLogger(WebResourceManagerImpl.class);

    static final String STATIC_RESOURCE_PREFIX = "s";
    static final String STATIC_RESOURCE_SUFFIX = "_";

    static final String REQUEST_CACHE_RESOURCE_KEY = "plugin.webresource.names";

    protected final WebResourceIntegration webResourceIntegration;
    protected final PluginResourceLocator pluginResourceLocator;
    protected final ResourceBatchingConfiguration batchingConfiguration;
    protected final ResourceDependencyResolver dependencyResolver;
    protected static final List<WebResourceFormatter> webResourceFormatters = Arrays.asList(CssWebResource.FORMATTER, JavascriptWebResource.FORMATTER);

    private static final boolean IGNORE_SUPERBATCHING = false;

    public WebResourceManagerImpl(PluginResourceLocator pluginResourceLocator, WebResourceIntegration webResourceIntegration) {
        this(pluginResourceLocator, webResourceIntegration, new DefaultResourceBatchingConfiguration());
    }

    public WebResourceManagerImpl(PluginResourceLocator pluginResourceLocator, WebResourceIntegration webResourceIntegration, ResourceBatchingConfiguration batchingConfiguration) {
        this(pluginResourceLocator, webResourceIntegration, batchingConfiguration, new DefaultResourceDependencyResolver(webResourceIntegration, batchingConfiguration));
    }

    public WebResourceManagerImpl(PluginResourceLocator pluginResourceLocator, WebResourceIntegration webResourceIntegration,
                                  ResourceBatchingConfiguration batchingConfiguration, ResourceDependencyResolver dependencyResolver) {
        this.pluginResourceLocator = pluginResourceLocator;
        this.webResourceIntegration = webResourceIntegration;
        this.batchingConfiguration = batchingConfiguration;
        this.dependencyResolver = dependencyResolver;
    }

    public void requireResource(String moduleCompleteKey) {
        log.debug("Requiring resource: " + moduleCompleteKey);
        getIncludedResourceNames().addAll(dependencyResolver.getDependencies(moduleCompleteKey, batchingConfiguration.isSuperBatchingEnabled()));
    }

    public void requireResourcesForContext(String context) {
        final List<WebResourceModuleDescriptor> webResourceModuleDescriptors =
                webResourceIntegration.getPluginAccessor().getEnabledModuleDescriptorsByClass(WebResourceModuleDescriptor.class);

        for (WebResourceModuleDescriptor webResourceModuleDescriptor : webResourceModuleDescriptors) {
            if (webResourceModuleDescriptor.getContexts().contains(context)) {
                requireResource(webResourceModuleDescriptor.getCompleteKey());
            }
        }
    }

    private LinkedHashSet<String> getIncludedResourceNames() {
        final Map<String, Object> cache = webResourceIntegration.getRequestCache();
        @SuppressWarnings("unchecked")
        LinkedHashSet<String> webResourceNames = (LinkedHashSet<String>) cache.get(REQUEST_CACHE_RESOURCE_KEY);
        if (webResourceNames == null) {
            webResourceNames = new LinkedHashSet<String>();
            cache.put(REQUEST_CACHE_RESOURCE_KEY, webResourceNames);
        }
        return webResourceNames;
    }

    private void clearIncludedResourceNames() {
        log.debug("Clearing included resources");
        getIncludedResourceNames().clear();
    }

    /**
     * This is the equivalent of of calling {@link #includeResources(Writer, UrlMode, WebResourceFilter)} with
     * {@link UrlMode#AUTO} and a {@link DefaultWebResourceFilter}.
     *
     * @see #includeResources(Writer, UrlMode, WebResourceFilter)
     */
    public void includeResources(Writer writer) {
        includeResources(writer, UrlMode.AUTO);
    }

    public void includeResources(Iterable<String> moduleCompleteKeys, Writer writer, UrlMode urlMode) {
        LinkedHashSet<String> resources = new LinkedHashSet<String>();
        for (String moduleCompleteKey : moduleCompleteKeys) {
            // Include resources from the super batch as we don't include the super batch itself
            Set<String> dependencies = dependencyResolver.getDependencies(moduleCompleteKey, false);
            resources.addAll(dependencies);
        }
        writeResourceTags(getModuleResources(resources, DefaultWebResourceFilter.INSTANCE), writer, urlMode);
    }

    /**
     * This is the equivalent of of calling {@link #includeResources(Writer, UrlMode, WebResourceFilter)} with
     * the given url mode and a {@link DefaultWebResourceFilter}.
     *
     * @see #includeResources(Writer, UrlMode, WebResourceFilter)
     */
    public void includeResources(Writer writer, UrlMode urlMode) {
        includeResources(writer, urlMode, DefaultWebResourceFilter.INSTANCE);
    }

    /**
     * Writes out the resource tags to the previously required resources called via requireResource methods for the
     * specified url mode and resource filter. Note that this method will clear the list of previously required resources.
     *
     * @param writer            the writer to write the links to
     * @param urlMode           the url mode to write resource url links in
     * @param webResourceFilter the resource filter to filter resources on
     * @since 2.4
     */
    public void includeResources(Writer writer, UrlMode urlMode, WebResourceFilter webResourceFilter) {
        writeIncludedResources(writer, urlMode, webResourceFilter);
        clearIncludedResourceNames();
    }

    /**
     * This is the equivalent of calling {@link #getRequiredResources(UrlMode, WebResourceFilter)} with
     * {@link UrlMode#AUTO} and a {@link DefaultWebResourceFilter}.
     *
     * @see #getRequiredResources(UrlMode, WebResourceFilter)
     */
    public String getRequiredResources() {
        return getRequiredResources(UrlMode.AUTO);
    }

    /**
     * This is the equivalent of calling {@link #getRequiredResources(UrlMode, WebResourceFilter)} with the given url
     * mode and a {@link DefaultWebResourceFilter}.
     *
     * @see #getRequiredResources(UrlMode, WebResourceFilter)
     */
    public String getRequiredResources(UrlMode urlMode) {
        return getRequiredResources(urlMode, DefaultWebResourceFilter.INSTANCE);
    }

    /**
     * Returns a String of the resources tags to the previously required resources called via requireResource methods
     * for the specified url mode and resource filter. Note that this method will NOT clear the list of previously
     * required resources.
     *
     * @param urlMode           the url mode to write out the resource tags
     * @param webResourceFilter the web resource filter to filter resources on
     * @return a String of the resource tags
     * @since 2.4
     */
    public String getRequiredResources(UrlMode urlMode, WebResourceFilter webResourceFilter) {
        final StringWriter writer = new StringWriter();
        writeIncludedResources(writer, urlMode, webResourceFilter);
        return writer.toString();
    }

    /**
     * Write all currently included resources to the given writer.
     */
    private void writeIncludedResources(Writer writer, UrlMode urlMode, WebResourceFilter filter) {
        List<PluginResource> resourcesToInclude = new ArrayList<PluginResource>();
        resourcesToInclude.addAll(getSuperBatchResources(filter));
        resourcesToInclude.addAll(getModuleResources(getIncludedResourceNames(), filter));

        writeResourceTags(resourcesToInclude, writer, urlMode);
    }

    /**
     * Get all super-batch resources that match the given filter. If superbatching is disabled this will just
     * return the empty list.
     * <p/>
     * Package private so it can be tested independently.
     */
    List<PluginResource> getSuperBatchResources(WebResourceFilter filter) {
        if (!batchingConfiguration.isSuperBatchingEnabled())
            return Collections.emptyList();

        LinkedHashSet<String> superBatchModuleKeys = dependencyResolver.getSuperBatchDependencies();
        List<PluginResource> resources = new ArrayList<PluginResource>();

        // This is necessarily quite complicated. We need distinct superbatch resources for each combination of
        // resourceFormatter (i.e. separate CSS or JS resources), and also each unique combination of
        // BATCH_PARAMS (i.e. separate superbatches for print stylesheets, IE only stylesheets, and IE only print
        // stylesheets if they ever exist in the future)
        for (WebResourceFormatter formatter : webResourceFormatters) {
            Set<Map<String, String>> alreadyIncluded = new HashSet<Map<String, String>>();
            for (String moduleKey : superBatchModuleKeys) {
                for (PluginResource pluginResource : pluginResourceLocator.getPluginResources(moduleKey)) {
                    if (formatter.matches(pluginResource.getResourceName()) && filter.matches(pluginResource.getResourceName())) {
                        Map<String, String> batchParamsMap = new HashMap<String, String>(PluginResourceLocator.BATCH_PARAMS.length);
                        for (String s : PluginResourceLocator.BATCH_PARAMS) {
                            batchParamsMap.put(s, pluginResource.getParams().get(s));
                        }

                        if (!alreadyIncluded.contains(batchParamsMap)) {
                            resources.add(SuperBatchPluginResource.createBatchFor(pluginResource));
                            alreadyIncluded.add(batchParamsMap);
                        }
                    }
                }
            }
        }
        return resources;
    }

    private List<PluginResource> getModuleResources(LinkedHashSet<String> webResourcePluginModuleKeys, WebResourceFilter filter) {
        List<PluginResource> includedResources = new ArrayList<PluginResource>();
        for (String moduleKey : webResourcePluginModuleKeys) {
            List<PluginResource> moduleResources = pluginResourceLocator.getPluginResources(moduleKey);
            for (PluginResource moduleResource : moduleResources) {
                if (filter.matches(moduleResource.getResourceName()))
                    includedResources.add(moduleResource);
                else
                    log.debug("Resource [" + moduleResource.getResourceName() + "] excluded by filter");
            }
        }
        return includedResources;
    }

    /**
     * Write the tags for the given set of resources to the writer. Writing will be done in order of
     * webResourceFormatters so that all CSS resources will be output before Javascript.
     */
    private void writeResourceTags(List<PluginResource> resourcesToInclude, Writer writer, UrlMode urlMode) {
        for (WebResourceFormatter formatter : webResourceFormatters) {
            for (Iterator<PluginResource> iter = resourcesToInclude.iterator(); iter.hasNext();) {
                PluginResource resource = iter.next();
                if (formatter.matches(resource.getResourceName())) {
                    writeResourceTag(urlMode, resource, formatter, writer);
                    iter.remove();
                }
            }
        }

        for (PluginResource resource : resourcesToInclude) {
            writeContentAndSwallowErrors("<!-- Error loading resource \"" + resource.getModuleCompleteKey() + "\".  No resource formatter matches \"" + resource.getResourceName() + "\" -->\n",
                    writer);
        }
    }

    private void writeResourceTag(UrlMode urlMode, PluginResource resource, WebResourceFormatter formatter, Writer writer) {
        String url = resource.getUrl();
        if (resource.isCacheSupported()) {
            url = getStaticResourcePrefix(resource.getVersion(webResourceIntegration), urlMode) + url;
        } else {
            url = webResourceIntegration.getBaseUrl(urlMode) + url;
        }
        writeContentAndSwallowErrors(formatter.formatResource(url, resource.getParams()), writer);
    }

    public void requireResource(String moduleCompleteKey, Writer writer) {
        requireResource(moduleCompleteKey, writer, UrlMode.AUTO);
    }

    public void requireResource(String moduleCompleteKey, Writer writer, UrlMode urlMode) {
        LinkedHashSet<String> allDependentModuleKeys = dependencyResolver.getDependencies(moduleCompleteKey, IGNORE_SUPERBATCHING);
        List<PluginResource> resourcesToInclude = getModuleResources(allDependentModuleKeys, DefaultWebResourceFilter.INSTANCE);
        writeResourceTags(resourcesToInclude, writer, urlMode);
    }

    public String getResourceTags(String moduleCompleteKey) {
        return getResourceTags(moduleCompleteKey, UrlMode.AUTO);
    }

    public String getResourceTags(String moduleCompleteKey, UrlMode urlMode) {
        final StringWriter writer = new StringWriter();
        requireResource(moduleCompleteKey, writer, urlMode);
        return writer.toString();
    }

    private void writeContentAndSwallowErrors(String content, Writer writer) {
        try {
            writer.write(content);
        }
        catch (final IOException e) {
            log.debug("Ignoring", e);
        }
    }

    public String getStaticResourcePrefix() {
        return getStaticResourcePrefix(UrlMode.AUTO);
    }

    public String getStaticResourcePrefix(UrlMode urlMode) {
        // "{base url}/s/{build num}/{system counter}/_"
        return webResourceIntegration.getBaseUrl(urlMode) + "/" + STATIC_RESOURCE_PREFIX + "/" + webResourceIntegration.getSystemBuildNumber() + "/" + webResourceIntegration.getSystemCounter() + "/" + STATIC_RESOURCE_SUFFIX;
    }

    public String getStaticResourcePrefix(String resourceCounter) {
        return getStaticResourcePrefix(resourceCounter, UrlMode.AUTO);
    }

    public String getStaticResourcePrefix(String resourceCounter, UrlMode urlMode) {
        // "{base url}/s/{build num}/{system counter}/{resource counter}/_"
        return webResourceIntegration.getBaseUrl(urlMode) + "/" + STATIC_RESOURCE_PREFIX + "/" + webResourceIntegration.getSystemBuildNumber() + "/" + webResourceIntegration.getSystemCounter() + "/" + resourceCounter + "/" + STATIC_RESOURCE_SUFFIX;
    }

    public String getStaticPluginResource(final String moduleCompleteKey, final String resourceName) {
        return getStaticPluginResource(moduleCompleteKey, resourceName, UrlMode.AUTO);
    }

    public String getStaticPluginResource(final String moduleCompleteKey, final String resourceName, final UrlMode urlMode) {
        final ModuleDescriptor<?> moduleDescriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(moduleCompleteKey);
        if (moduleDescriptor == null) {
            return null;
        }

        return getStaticPluginResource(moduleDescriptor, resourceName, urlMode);
    }

    /**
     * @return "{base url}/s/{build num}/{system counter}/{plugin version}/_/download/resources/{plugin.key:module.key}/{resource.name}"
     */
    @SuppressWarnings("unchecked")
    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName) {
        return getStaticPluginResource(moduleDescriptor, resourceName, UrlMode.AUTO);
    }

    public String getStaticPluginResource(ModuleDescriptor moduleDescriptor, String resourceName, UrlMode urlMode) {
        // "{base url}/s/{build num}/{system counter}/{plugin version}/_"
        final String staticUrlPrefix = getStaticResourcePrefix(String.valueOf(moduleDescriptor.getPlugin().getPluginsVersion()), urlMode);
        // "/download/resources/plugin.key:module.key/resource.name"
        return staticUrlPrefix + pluginResourceLocator.getResourceUrl(moduleDescriptor.getCompleteKey(), resourceName);
    }

    /* Deprecated methods */

    /**
     * @deprecated Use {@link #getStaticPluginResource(org.maera.plugin.ModuleDescriptor, String)} instead
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public String getStaticPluginResourcePrefix(ModuleDescriptor moduleDescriptor, String resourceName) {
        return getStaticPluginResource(moduleDescriptor, resourceName);
    }

    /**
     * @deprecated Since 2.2
     */
    @Deprecated
    private static final String REQUEST_CACHE_MODE_KEY = "plugin.webresource.mode";

    /**
     * @deprecated Since 2.2
     */
    @Deprecated
    private static final IncludeMode DEFAULT_INCLUDE_MODE = WebResourceManager.DELAYED_INCLUDE_MODE;

    /**
     * @deprecated Since 2.2.
     */
    @Deprecated
    public void setIncludeMode(final IncludeMode includeMode) {
        webResourceIntegration.getRequestCache().put(REQUEST_CACHE_MODE_KEY, includeMode);
    }
}
