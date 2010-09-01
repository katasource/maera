package org.maera.plugin.osgi.container;

import java.io.File;

/**
 * Access to persistent cache locations used throughout the OSGi plugin system.  Implementations are responsible for
 * ensuring the directories are not null and do exist.
 *
 * @since 0.1
 */
public interface OsgiPersistentCache {

    /**
     * @return the directory to store extracted framework bundles
     */
    File getFrameworkBundleCache();

    /**
     * @return the directory to use for the container bundle cache
     */
    File getOsgiBundleCache();

    /**
     * @return the directory to store transformed plugins
     */
    File getTransformedPluginCache();

    /**
     * Clear all caches
     *
     * @throws OsgiContainerException If the caches couldn't be cleared
     */
    void clear() throws OsgiContainerException;

    /**
     * Validates the caches against a cache key.  If the key changes, the directories are wiped clean.
     *
     * @param cacheKey The cache key, can be anything
     */
    void validate(String cacheKey);
}
