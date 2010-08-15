package org.maera.plugin.webresource;

import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.Resources;
import org.maera.plugin.Resources.TypeFilter;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.servlet.*;
import org.maera.plugin.util.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.google.common.collect.Iterables.filter;
import static org.maera.plugin.util.EfficientStringUtils.endsWith;

/**
 * Default implementation of {@link PluginResourceLocator}.
 *
 * @since 2.2
 */
public class PluginResourceLocatorImpl implements PluginResourceLocator {
    private static final Logger log = LoggerFactory.getLogger(PluginResourceLocatorImpl.class);

    public static final String PLUGIN_WEBRESOURCE_BATCHING_OFF = "plugin.webresource.batching.off";

    private static final String DOWNLOAD_TYPE = "download";

    final private PluginAccessor pluginAccessor;
    final private ServletContextFactory servletContextFactory;
    final private ResourceDependencyResolver dependencyResolver;

    private static final String RESOURCE_SOURCE_PARAM = "source";
    private static final String RESOURCE_BATCH_PARAM = "batch";

    public PluginResourceLocatorImpl(final WebResourceIntegration webResourceIntegration, final ServletContextFactory servletContextFactory) {
        this(webResourceIntegration, servletContextFactory, new DefaultResourceDependencyResolver(webResourceIntegration, new DefaultResourceBatchingConfiguration()));
    }

    public PluginResourceLocatorImpl(final WebResourceIntegration webResourceIntegration, final ServletContextFactory servletContextFactory,
                                     final ResourceBatchingConfiguration resourceBatchingConfiguration) {
        this(webResourceIntegration, servletContextFactory, new DefaultResourceDependencyResolver(webResourceIntegration, resourceBatchingConfiguration));
    }

    private PluginResourceLocatorImpl(final WebResourceIntegration webResourceIntegration, final ServletContextFactory servletContextFactory,
                                      final ResourceDependencyResolver dependencyResolver) {
        this.pluginAccessor = webResourceIntegration.getPluginAccessor();
        this.servletContextFactory = servletContextFactory;
        this.dependencyResolver = dependencyResolver;
    }

    public boolean matches(final String url) {
        return SuperBatchPluginResource.matches(url) || SuperBatchSubResource.matches(url) || SinglePluginResource.matches(url) || BatchPluginResource.matches(url);
    }

    public DownloadableResource getDownloadableResource(final String url, final Map<String, String> queryParams) {
        try {
            if (SuperBatchPluginResource.matches(url)) {
                return locateSuperBatchPluginResource(SuperBatchPluginResource.parse(url, queryParams));
            }
            if (SuperBatchSubResource.matches(url)) {
                return locateSuperBatchSubPluginResource(SuperBatchSubResource.parse(url, queryParams));
            }
            if (BatchPluginResource.matches(url)) {
                return locateBatchPluginResource(BatchPluginResource.parse(url, queryParams));
            }
            if (SinglePluginResource.matches(url)) {
                final SinglePluginResource resource = SinglePluginResource.parse(url);
                return locatePluginResource(resource.getModuleCompleteKey(), resource.getResourceName());
            }
        }
        catch (final UrlParseException e) {
            log.error("Unable to parse URL: " + url, e);
        }
        // TODO: It would be better to use Exceptions rather than returning
        // nulls to indicate an error.
        return null;
    }

    private DownloadableResource locateSuperBatchPluginResource(final SuperBatchPluginResource batchResource) {
        if (log.isDebugEnabled()) {
            log.debug(batchResource.toString());
        }

        for (final String moduleKey : dependencyResolver.getSuperBatchDependencies()) {
            final ModuleDescriptor<?> moduleDescriptor = pluginAccessor.getEnabledPluginModule(moduleKey);
            if (moduleDescriptor == null) {
                log.info("Resource batching configuration refers to plugin that does not exist: " + moduleKey);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("searching resources in: " + moduleKey);
                }

                for (final ResourceDescriptor resourceDescriptor : filter(moduleDescriptor.getResourceDescriptors(), new Resources.TypeFilter(DOWNLOAD_TYPE))) {
                    if (isResourceInBatch(resourceDescriptor, batchResource)) {
                        batchResource.add(locatePluginResource(moduleDescriptor.getCompleteKey(), resourceDescriptor.getName()));
                    }
                }
            }
        }
        return batchResource;
    }

    private DownloadableResource locateSuperBatchSubPluginResource(final SuperBatchSubResource superBatchSubResource) {
        for (final String moduleKey : dependencyResolver.getSuperBatchDependencies()) {
            final DownloadableResource pluginResource = locatePluginResource(moduleKey, superBatchSubResource.getResourceName());
            if (pluginResource != null) {
                return pluginResource;
            }
        }
        log.warn("Could not locate resource in superbatch: " + superBatchSubResource.getResourceName());
        return superBatchSubResource;
    }

    private DownloadableResource locateBatchPluginResource(final BatchPluginResource batchResource) {
        final ModuleDescriptor<?> moduleDescriptor = pluginAccessor.getEnabledPluginModule(batchResource.getModuleCompleteKey());
        for (final ResourceDescriptor resourceDescriptor : Iterables.filter(moduleDescriptor.getResourceDescriptors(), new TypeFilter(DOWNLOAD_TYPE))) {
            if (isResourceInBatch(resourceDescriptor, batchResource)) {
                batchResource.add(locatePluginResource(moduleDescriptor.getCompleteKey(), resourceDescriptor.getName()));
            }
        }

        // if batch is empty, check if we can locate a plugin resource
        if (batchResource.isEmpty()) {
            final DownloadableResource resource = locatePluginResource(batchResource.getModuleCompleteKey(), batchResource.getResourceName());
            if (resource != null) {
                return resource;
            }
        }

        return batchResource;
    }

    private boolean isResourceInBatch(final ResourceDescriptor resourceDescriptor, final BatchResource batchResource) {
        if (!descriptorTypeMatchesResourceType(resourceDescriptor, batchResource.getType())) {
            return false;
        }

        if (skipBatch(resourceDescriptor)) {
            return false;
        }

        for (final String param : BATCH_PARAMS) {
            final String batchValue = batchResource.getParams().get(param);
            final String resourceValue = resourceDescriptor.getParameter(param);

            if ((batchValue == null) && (resourceValue != null)) {
                return false;
            }

            if ((batchValue != null) && !batchValue.equals(resourceValue)) {
                return false;
            }
        }

        return true;
    }

    private boolean descriptorTypeMatchesResourceType(final ResourceDescriptor resourceDescriptor, final String type) {
        return endsWith(resourceDescriptor.getName(), ".", type);
    }

    private DownloadableResource locatePluginResource(final String moduleCompleteKey, final String resourceName) {
        DownloadableResource resource;

        // resource from the module
        if (moduleCompleteKey.indexOf(":") > -1) {
            final ModuleDescriptor<?> moduleDescriptor = pluginAccessor.getEnabledPluginModule(moduleCompleteKey);
            if (moduleDescriptor != null) {
                resource = getResourceFromModule(moduleDescriptor, resourceName, "");
            } else {
                log.info("Module not found: " + moduleCompleteKey);
                return null;
            }
        } else
        // resource from plugin
        {
            resource = getResourceFromPlugin(pluginAccessor.getPlugin(moduleCompleteKey), resourceName, "");
        }

        if (resource == null) {
            resource = getResourceFromPlugin(getPlugin(moduleCompleteKey), resourceName, "");
        }

        if (resource == null) {
            log.info("Unable to find resource for plugin: " + moduleCompleteKey + " and path: " + resourceName);
            return null;
        }

        return resource;
    }

    private Plugin getPlugin(final String moduleKey) {
        if ((moduleKey.indexOf(':') < 0) || (moduleKey.indexOf(':') == moduleKey.length() - 1)) {
            return null;
        }

        return pluginAccessor.getPlugin(moduleKey.substring(0, moduleKey.indexOf(':')));
    }

    private DownloadableResource getResourceFromModule(final ModuleDescriptor<?> moduleDescriptor, final String resourcePath, final String filePath) {
        final Plugin plugin = pluginAccessor.getPlugin(moduleDescriptor.getPluginKey());
        final ResourceLocation resourceLocation = moduleDescriptor.getResourceLocation(DOWNLOAD_TYPE, resourcePath);

        if (resourceLocation != null) {
            boolean disableMinification = false;
            // I think it should always be a WebResourceModuleDescriptor, but
            // not sure...
            if (moduleDescriptor instanceof WebResourceModuleDescriptor) {
                disableMinification = ((WebResourceModuleDescriptor) moduleDescriptor).isDisableMinification();
            }
            return getDownloadablePluginResource(plugin, resourceLocation, moduleDescriptor, filePath, disableMinification);
        }

        final String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null) {
            return null;
        }

        return getResourceFromModule(moduleDescriptor, nextParts[0], nextParts[1] + filePath);
    }

    private DownloadableResource getResourceFromPlugin(final Plugin plugin, final String resourcePath, final String filePath) {
        if (plugin == null) {
            return null;
        }

        final ResourceLocation resourceLocation = plugin.getResourceLocation(DOWNLOAD_TYPE, resourcePath);
        if (resourceLocation != null) {
            return getDownloadablePluginResource(plugin, resourceLocation, null, filePath, false);
        }

        final String[] nextParts = splitLastPathPart(resourcePath);
        if (nextParts == null) {
            return null;
        }

        return getResourceFromPlugin(plugin, nextParts[0], nextParts[1] + filePath);
    }

    // pacakge protected so we can test it

    String[] splitLastPathPart(final String resourcePath) {
        int indexOfSlash = resourcePath.lastIndexOf('/');
        if (resourcePath.endsWith("/")) // skip over the trailing slash
        {
            indexOfSlash = resourcePath.lastIndexOf('/', indexOfSlash - 1);
        }

        if (indexOfSlash < 0) {
            return null;
        }

        return new String[]{resourcePath.substring(0, indexOfSlash + 1), resourcePath.substring(indexOfSlash + 1)};
    }

    private DownloadableResource getDownloadablePluginResource(final Plugin plugin, final ResourceLocation resourceLocation,
                                                               ModuleDescriptor descriptor, final String filePath,
                                                               final boolean disableMinification) {
        final String sourceParam = resourceLocation.getParameter(RESOURCE_SOURCE_PARAM);

        // serve by forwarding the request to the location - batching not
        // supported
        if ("webContext".equalsIgnoreCase(sourceParam)) {
            return new ForwardableResource(resourceLocation);
        }

        DownloadableResource actualResource = null;
        // serve static resources from the web application - batching supported
        if ("webContextStatic".equalsIgnoreCase(sourceParam)) {
            actualResource = new DownloadableWebResource(plugin, resourceLocation, filePath, servletContextFactory.getServletContext(), disableMinification);
        } else {
            actualResource = new DownloadableClasspathResource(plugin, resourceLocation, filePath);
        }

        DownloadableResource result = actualResource;
        // web resources are able to be transformed during delivery
        if (descriptor instanceof WebResourceModuleDescriptor) {
            DownloadableResource lastResource = actualResource;
            WebResourceModuleDescriptor desc = (WebResourceModuleDescriptor) descriptor;
            for (WebResourceTransformation list : desc.getTransformations()) {
                if (list.matches(resourceLocation)) {
                    lastResource = list.transformDownloadableResource(pluginAccessor, actualResource, resourceLocation, filePath);
                }
            }
            result = lastResource;
        }
        return result;
    }

    public List<PluginResource> getPluginResources(final String moduleCompleteKey) {
        final ModuleDescriptor<?> moduleDescriptor = pluginAccessor.getEnabledPluginModule(moduleCompleteKey);
        if ((moduleDescriptor == null) || !(moduleDescriptor instanceof WebResourceModuleDescriptor)) {
            log.error("Error loading resource \"" + moduleCompleteKey + "\". Resource is not a Web Resource Module");
            return Collections.emptyList();
        }

        final boolean singleMode = isBatchingOff();
        final List<PluginResource> resources = new ArrayList<PluginResource>();

        for (final ResourceDescriptor resourceDescriptor : moduleDescriptor.getResourceDescriptors()) {
            if (singleMode || skipBatch(resourceDescriptor)) {
                final boolean cache = !"false".equalsIgnoreCase(resourceDescriptor.getParameter("cache"));
                resources.add(new SinglePluginResource(resourceDescriptor.getName(), moduleDescriptor.getCompleteKey(), cache, resourceDescriptor.getParameters()));
            } else {
                final BatchPluginResource batchResource = createBatchResource(moduleDescriptor.getCompleteKey(), resourceDescriptor);
                if (!resources.contains(batchResource)) {
                    resources.add(batchResource);
                }
            }
        }
        return resources;
    }

    /**
     * @return True if either it is explicitly turned off or in dev mode
     */
    Boolean isBatchingOff() {
        String explicitSetting = System.getProperty(PLUGIN_WEBRESOURCE_BATCHING_OFF);
        if (explicitSetting != null) {
            return Boolean.parseBoolean(explicitSetting);
        } else {
            return Boolean.parseBoolean(System.getProperty(PluginUtils.ATLASSIAN_DEV_MODE));
        }

    }

    private boolean skipBatch(final ResourceDescriptor resourceDescriptor) {
        // you can't batch forwarded requests
        return "false".equalsIgnoreCase(resourceDescriptor.getParameter(RESOURCE_BATCH_PARAM))
                || "webContext".equalsIgnoreCase(resourceDescriptor.getParameter(RESOURCE_SOURCE_PARAM));
    }

    private BatchPluginResource createBatchResource(final String moduleCompleteKey, final ResourceDescriptor resourceDescriptor) {
        final String name = resourceDescriptor.getName();
        final String type = name.substring(name.lastIndexOf(".") + 1);
        final Map<String, String> params = new TreeMap<String, String>();
        for (final String param : BATCH_PARAMS) {
            final String value = resourceDescriptor.getParameter(param);
            if (StringUtils.isNotEmpty(value)) {
                params.put(param, value);
            }
        }

        return new BatchPluginResource(moduleCompleteKey, type, params);
    }

    public String getResourceUrl(final String moduleCompleteKey, final String resourceName) {
        return new SinglePluginResource(resourceName, moduleCompleteKey, false).getUrl();
    }
}
