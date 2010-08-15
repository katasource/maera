package org.maera.plugin.webresource;

import java.util.LinkedHashSet;

interface ResourceDependencyResolver {
    /**
     * Returns an ordered set of the super batch resources and its dependencies.
     * Implementations are expected to use the {@link ResourceBatchingConfiguration} provided.
     */
    public LinkedHashSet<String> getSuperBatchDependencies();

    /**
     * Returns an ordered set of the given resource and its dependencies. To exclude resource dependencies
     * in the super batch, pass excludeSuperBatchedResources as true.
     *
     * @param moduleKey                    the complete module key of the web resource to retrieve dependencies for
     * @param excludeSuperBatchedResources whether or not to exclude resources that are part of the super batch.
     */
    public LinkedHashSet<String> getDependencies(String moduleKey, boolean excludeSuperBatchedResources);
}
