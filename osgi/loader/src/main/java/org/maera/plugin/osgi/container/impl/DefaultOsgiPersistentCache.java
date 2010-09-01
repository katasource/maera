package org.maera.plugin.osgi.container.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.maera.plugin.osgi.container.OsgiContainerException;
import org.maera.plugin.osgi.container.OsgiPersistentCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Default implementation of persistent cache.  Handles clearing of directories if an upgrade has been detected.
 *
 * @since 0.1
 */
public class DefaultOsgiPersistentCache implements OsgiPersistentCache {
    
    private final File osgiBundleCache;
    private final File frameworkBundleCache;
    private final File transformedPluginCache;
    private final Logger log = LoggerFactory.getLogger(DefaultOsgiPersistentCache.class);

    /**
     * Constructs a cache, using the passed file as the base directory for cache subdirectories
     *
     * @param baseDir The base directory
     */
    public DefaultOsgiPersistentCache(final File baseDir) {
        Validate.notNull(baseDir, "The base directory for OSGi caches cannot be null");
        Validate.isTrue(baseDir.exists(), "The base directory for OSGi persistent caches should exist");
        osgiBundleCache = new File(baseDir, "felix");
        frameworkBundleCache = new File(baseDir, "framework-bundles");
        transformedPluginCache = new File(baseDir, "transformed-plugins");
        validate(null);
    }

    /**
     * Constructor added in the 2.2.0 beta timeframe, but was made redundant later.  Application version is not used.
     *
     * @deprecated
     */
    @Deprecated
    public DefaultOsgiPersistentCache(final File baseDir, final String applicationVersion) {
        this(baseDir);
    }

    public File getFrameworkBundleCache() {
        return frameworkBundleCache;
    }

    public File getOsgiBundleCache() {
        return osgiBundleCache;
    }

    public File getTransformedPluginCache() {
        return transformedPluginCache;
    }

    public void clear() throws OsgiContainerException {
        try {
            FileUtils.cleanDirectory(frameworkBundleCache);
            FileUtils.cleanDirectory(osgiBundleCache);
            FileUtils.cleanDirectory(transformedPluginCache);
        }
        catch (final IOException e) {
            throw new OsgiContainerException("Unable to clear OSGi caches", e);
        }
    }

    public void validate(final String cacheValidationKey) {
        ensureDirectoryExists(frameworkBundleCache);
        ensureDirectoryExists(osgiBundleCache);
        ensureDirectoryExists(transformedPluginCache);

        try {
            FileUtils.cleanDirectory(osgiBundleCache);
        }
        catch (final IOException e) {
            throw new OsgiContainerException("Unable to clean the cache directory: " + osgiBundleCache, e);
        }

        if (cacheValidationKey != null) {
            final File versionFile = new File(transformedPluginCache, "cache.key");
            if (versionFile.exists()) {
                String oldVersion = null;
                try {
                    oldVersion = FileUtils.readFileToString(versionFile);
                }
                catch (final IOException e) {
                    log.debug("Unable to read cache key file", e);
                }
                if (!cacheValidationKey.equals(oldVersion)) {
                    log.info("Application upgrade detected, clearing OSGi cache directories");
                    clear();
                } else {
                    return;
                }
            }

            try {
                FileUtils.writeStringToFile(versionFile, cacheValidationKey);
            }
            catch (final IOException e) {
                log.warn("Unable to write cache key file, so will be unable to detect upgrades", e);
            }
        }
    }

    private void ensureDirectoryExists(final File dir) {
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalArgumentException("'" + dir + "' is not a directory");
        }

        if (!dir.exists() && !dir.mkdir()) {
            throw new IllegalArgumentException("Directory '" + dir + "' cannot be created");
        }
    }
}
