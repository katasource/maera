package org.maera.plugin.osgi.container;

import javax.servlet.ServletContext;
import java.util.List;
import java.util.Map;

/**
 * Contains configuration for the package scanning algorithm that scans your classpath to determine which packages
 * and versions to export to OSGi.  Jar and package includes/excludes, and packages for the package version map can
 * either be simple names or wildcard patterns, where the "*" character will match any character.
 * <p/>
 * <p/>
 * Includes and excludes are matched so that only includes are, well, included, but if you need to filter a few out
 * of that set, the exclude patterns will be removed.
 */
public interface PackageScannerConfiguration {
    /**
     * @return The jar patterns to include
     */
    List<String> getJarIncludes();

    /**
     * @return The jar patterns to exclude
     */
    List<String> getJarExcludes();

    /**
     * @return The package patterns to include
     */
    List<String> getPackageIncludes();

    /**
     * @return The package patterns to exclude
     */
    List<String> getPackageExcludes();

    /**
     * @return A map of package patterns and their versions
     */
    Map<String, String> getPackageVersions();

    /**
     * @return The current host application version number.  Used as a caching key for scanned data.
     * @since 2.2.0
     */
    String getCurrentHostVersion();

    /**
     * @return The servlet context to use to scan for jars, in case the classloader scanning fails
     */
    ServletContext getServletContext();
}
