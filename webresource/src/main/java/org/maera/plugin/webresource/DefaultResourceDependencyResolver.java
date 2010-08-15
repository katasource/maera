package org.maera.plugin.webresource;

import org.maera.plugin.ModuleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class DefaultResourceDependencyResolver implements ResourceDependencyResolver {
    private static final Logger log = LoggerFactory.getLogger(DefaultResourceDependencyResolver.class);

    private final WebResourceIntegration webResourceIntegration;
    private final ResourceBatchingConfiguration batchingConfiguration;

    private String superBatchVersion;
    private LinkedHashSet<String> superBatchResources;

    public DefaultResourceDependencyResolver(WebResourceIntegration webResourceIntegration, ResourceBatchingConfiguration batchingConfiguration) {
        this.webResourceIntegration = webResourceIntegration;
        this.batchingConfiguration = batchingConfiguration;
    }

    public LinkedHashSet<String> getSuperBatchDependencies() {
        if (!batchingConfiguration.isSuperBatchingEnabled()) {
            log.warn("Super batching not enabled, but getSuperBatchDependencies() called. Returning empty set.");
            return new LinkedHashSet<String>();
        }

        String version = webResourceIntegration.getSuperBatchVersion();
        if (superBatchVersion == null || !superBatchVersion.equals(version)) {
            LinkedHashSet<String> webResourceNames = new LinkedHashSet<String>();
            if (batchingConfiguration.getSuperBatchModuleCompleteKeys() != null) {
                for (String moduleKey : batchingConfiguration.getSuperBatchModuleCompleteKeys()) {
                    resolveDependencies(moduleKey, webResourceNames, Collections.emptySet(), new Stack<String>());
                }
            }
            synchronized (this) {
                superBatchResources = webResourceNames;
                superBatchVersion = version;
            }
        }
        return new LinkedHashSet(superBatchResources);
    }

    public LinkedHashSet<String> getDependencies(String moduleKey, boolean excludeSuperBatchedResources) {
        LinkedHashSet<String> orderedResourceKeys = new LinkedHashSet<String>();
        Set<String> superBatchResources = excludeSuperBatchedResources ? getSuperBatchDependencies() : Collections.<String>emptySet();
        resolveDependencies(moduleKey, orderedResourceKeys, superBatchResources, new Stack<String>());
        return orderedResourceKeys;
    }

    /**
     * Adds the resources as well as its dependencies in order to the given ordered set. This method uses recursion
     * to add a resouce's dependent resources also to the set. You should call this method with a new stack
     * passed to the last parameter.
     * <p/>
     * Note that resources already in the given super batch will be exluded when resolving dependencies. You
     * should pass in an empty set for the super batch to include super batch resources.
     *
     * @param moduleKey           the module complete key to add as well as its dependencies
     * @param orderedResourceKeys an ordered list set where the resources are added in order
     * @param superBatchResources the set of super batch resources to exclude when resolving dependencies
     * @param stack               where we are in the dependency tree
     */
    private void resolveDependencies(final String moduleKey, final LinkedHashSet<String> orderedResourceKeys,
                                     final Set superBatchResources, final Stack<String> stack) {
        if (superBatchResources.contains(moduleKey)) {
            log.debug("Not requiring resource: " + moduleKey + " because it is part of a super-batch");
            return;
        }
        if (stack.contains(moduleKey)) {
            log.warn("Cyclic plugin resource dependency has been detected with: " + moduleKey + "\n" + "Stack trace: " + stack);
            return;
        }

        final ModuleDescriptor<?> moduleDescriptor = webResourceIntegration.getPluginAccessor().getEnabledPluginModule(moduleKey);
        if (!(moduleDescriptor instanceof WebResourceModuleDescriptor)) {
            if (webResourceIntegration.getPluginAccessor().getPluginModule(moduleKey) != null)
                log.warn("Cannot include disabled web resource module: " + moduleKey);
            else
                log.warn("Cannot find web resource module for: " + moduleKey);
            return;
        }

        final List<String> dependencies = ((WebResourceModuleDescriptor) moduleDescriptor).getDependencies();
        if (log.isDebugEnabled()) {
            log.debug("About to add resource [" + moduleKey + "] and its dependencies: " + dependencies);
        }
        stack.push(moduleKey);
        try {
            for (final String dependency : dependencies) {
                if (!orderedResourceKeys.contains(dependency)) {
                    resolveDependencies(dependency, orderedResourceKeys, superBatchResources, stack);
                }
            }
        }
        finally {
            stack.pop();
        }
        orderedResourceKeys.add(moduleKey);
    }
}
