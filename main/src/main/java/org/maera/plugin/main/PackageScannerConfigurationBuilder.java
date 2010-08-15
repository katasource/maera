package org.maera.plugin.main;

import org.maera.plugin.osgi.container.PackageScannerConfiguration;
import org.maera.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;

import javax.servlet.ServletContext;
import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * The builder for {@link PackageScannerConfiguration} instances that additionally performs validation and default creation.
 * For a usage example, see the package javadocs.
 * <p/>
 * Not thread-safe. Instances of this class should be thread and preferably method local.
 *
 * @since 2.2
 */
public class PackageScannerConfigurationBuilder {
    /**
     * Static factory for creating a new builder.
     *
     * @return a new builder.
     */
    public static PackageScannerConfigurationBuilder packageScannerConfiguration() {
        return new PackageScannerConfigurationBuilder();
    }

    private String hostVersion;
    private final List<String> jarIncludes = new ArrayList<String>();
    private final List<String> jarExcludes = new ArrayList<String>();
    private final List<String> packageIncludes = new ArrayList<String>();
    private final List<String> packageExcludes = new ArrayList<String>();
    private final Map<String, String> packageVersions = new HashMap<String, String>();
    private ServletContext servletContext;

    /**
     * Default constructor. Uses a DefaultPackageScannerConfiguration as the template.
     */
    public PackageScannerConfigurationBuilder() {
        copy(new DefaultPackageScannerConfiguration());
    }

    /**
     * Copy constructor. Use the supplied PackageScannerConfiguration as a template.
     *
     * @param packageScannerConfiguration
     */
    public PackageScannerConfigurationBuilder(final PackageScannerConfiguration packageScannerConfiguration) {
        copy(packageScannerConfiguration);
    }

    private void copy(final PackageScannerConfiguration config) {
        hostVersion = config.getCurrentHostVersion();
        packageIncludes.addAll(config.getPackageIncludes());
        packageExcludes.addAll(config.getPackageExcludes());
        jarIncludes.addAll(config.getJarIncludes());
        jarExcludes.addAll(config.getJarExcludes());
        packageVersions.putAll(config.getPackageVersions());
        servletContext = config.getServletContext();
    }

    /**
     * Sets the current host version.
     *
     * @param pkgs A list of package expressions, where the '*' character matches any character including subpackages
     * @return this
     * @see PackageScannerConfiguration#getCurrentHostVersion()
     */
    public PackageScannerConfigurationBuilder hostVersion(final String... pkgs) {
        packageIncludes.addAll(Arrays.asList(pkgs));
        return this;
    }

    /**
     * Sets the {@link ServletContext} used to lookup jars as some application servers need it to lookup jar files.
     *
     * @param pkgs A list of package expressions, where the '*' character matches any character including subpackages
     * @return this
     * @see PackageScannerConfiguration#getS()
     */
    public PackageScannerConfigurationBuilder servletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
        return this;
    }

    /**
     * Sets a list of package expressions to expose to plugins.
     *
     * @param pkgs A list of package expressions, where the '*' character matches any character including subpackages
     * @return this
     * @see PackageScannerConfiguration#getPackageIncludes()
     */
    public PackageScannerConfigurationBuilder packagesToInclude(final String... pkgs) {
        packageIncludes.addAll(Arrays.asList(pkgs));
        return this;
    }

    /**
     * Sets a list of package expressions to hide from plugins.
     *
     * @param pkgs A list of package expressions, where the '*' character matches any character including subpackages
     * @return this
     * @see PackageScannerConfiguration#getPackageExcludes()
     */
    public PackageScannerConfigurationBuilder packagesToExclude(final String... pkgs) {
        packageExcludes.addAll(Arrays.asList(pkgs));
        return this;
    }

    /**
     * Sets which packages should be exposed as which versions.
     *
     * @param packageToVersion A map of package names to version names.  No wildcards allowed, and the version names
     *                         must match the expected OSGi versioning scheme.
     * @return this
     * @see PackageScannerConfiguration#getPackageVersions()
     */
    public PackageScannerConfigurationBuilder packagesVersions(final Map<String, String> packageToVersion) {
        packageVersions.putAll(packageToVersion);
        return this;
    }

    /**
     * Sets a list of jar expressions to expose to plugins.
     *
     * @param pkgs A list of package expressions, where the '*' character matches any character including subpackages
     * @return this
     * @see PackageScannerConfiguration#getJarIncludes()
     */
    public PackageScannerConfigurationBuilder jarsToInclude(final String... jars) {
        jarIncludes.addAll(Arrays.asList(jars));
        return this;
    }

    /**
     * Sets a list of jar expressions to hide from plugins.
     *
     * @param pkgs A list of package expressions, where the '*' character matches any character including subpackages
     * @return this
     * @see PackageScannerConfiguration#getPackageExcludes()
     */
    public PackageScannerConfigurationBuilder jarsToExclude(final String... jars) {
        jarExcludes.addAll(Arrays.asList(jars));
        return this;
    }

    /**
     * Builds a {@link PackageScannerConfiguration} instance by processing the configuration that
     * was previously set and setting any defaults where not explicitly specified.
     *
     * @return A valid {@link PackageScannerConfiguration} instance to pass to {@link AtlassianPlugins}
     */
    public PackageScannerConfiguration build() {
        return new ImmutablePackageScannerConfiguration(this);
    }

    //
    // inner classes
    //

    /**
     * Immutable and thread-safe implementation of the {@link PackageScannerConfiguration} returned by a
     * {@link PackageScannerConfigurationBuilder}
     */
    private static final class ImmutablePackageScannerConfiguration implements PackageScannerConfiguration {
        private final String hostVersion;
        private final List<String> jarIncludes;
        private final List<String> jarExcludes;
        private final List<String> packageIncludes;
        private final List<String> packageExcludes;
        private final Map<String, String> packageVersions;
        private final ServletContext servletContext;

        ImmutablePackageScannerConfiguration(final PackageScannerConfigurationBuilder builder) {
            hostVersion = builder.hostVersion;
            jarIncludes = unmodifiableList(new ArrayList<String>(builder.jarIncludes));
            jarExcludes = unmodifiableList(new ArrayList<String>(builder.jarExcludes));
            packageIncludes = unmodifiableList(new ArrayList<String>(builder.packageIncludes));
            packageExcludes = unmodifiableList(new ArrayList<String>(builder.packageExcludes));
            packageVersions = unmodifiableMap(new HashMap<String, String>(builder.packageVersions));
            servletContext = builder.servletContext;
        }

        public List<String> getJarIncludes() {
            return jarIncludes;
        }

        public List<String> getJarExcludes() {
            return jarExcludes;
        }

        public List<String> getPackageIncludes() {
            return packageIncludes;
        }

        public List<String> getPackageExcludes() {
            return packageExcludes;
        }

        public Map<String, String> getPackageVersions() {
            return packageVersions;
        }

        public String getCurrentHostVersion() {
            return hostVersion;
        }

        public ServletContext getServletContext() {
            return servletContext;
        }
    }
}
