package org.maera.plugin.webresource;

import java.util.List;

/**
 * Resource batching configuration for the {@link WebResourceManagerImpl}.
 * <p/>
 * Currently contains the configuration for super batch support.
 */
public interface ResourceBatchingConfiguration {
    /**
     * Gets whether web resources in different resource modules should be batched together.
     */
    boolean isSuperBatchingEnabled();

    /**
     * Gets the list of resource plugin modules that should be included in the superbatch, in the order that
     * they should be batched. No dependency resolution is performed, so it is important that the configuration
     * includes all dependent resources in the right order.
     * <p/>
     * Any call to {@link WebResourceManager#requireResource} for one of these resources will be a no-op,
     * and any dependency resolution for resources will stop if the dependency is in the superbatch.
     */
    List<String> getSuperBatchModuleCompleteKeys();
}
