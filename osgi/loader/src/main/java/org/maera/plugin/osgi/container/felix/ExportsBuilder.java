package org.maera.plugin.osgi.container.felix;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;
import org.apache.commons.io.IOUtils;
import org.maera.plugin.osgi.container.PackageScannerConfiguration;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.osgi.util.OsgiHeaderUtil;
import org.maera.plugin.util.ClassLoaderUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.twdata.pkgscanner.ExportPackage;
import org.twdata.pkgscanner.PackageScanner;

import javax.servlet.ServletContext;
import java.io.*;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import static org.twdata.pkgscanner.PackageScanner.*;

/**
 * Builds the OSGi package exports string.  Uses a file to cache the scanned results, keyed by the application version.
 */
class ExportsBuilder {

    static final String JDK_PACKAGES_PATH = "jdk-packages.txt";
    static final String JDK6_PACKAGES_PATH = "jdk6-packages.txt";
    private static Logger log = LoggerFactory.getLogger(ExportsBuilder.class);
    private static String exportStringCache;

    /**
     * Gets the framework exports taking into account host components and package scanner configuration.
     * <p/>
     * This information cannot change without a system restart, so we determine this once and then cache the value.
     * The cache is only useful if the plugin system is thrown away and re-initialised. This is done thousands of times
     * during JIRA functional testing, and the cache was added to speed this up.
     *
     * @param regs                 The list of host component registrations
     * @param packageScannerConfig The configuration for the package scanning
     * @return A list of exports, in a format compatible with OSGi headers
     */
    public String getExports(List<HostComponentRegistration> regs, PackageScannerConfiguration packageScannerConfig) {
        if (exportStringCache == null) {
            exportStringCache = determineExports(regs, packageScannerConfig);
        }
        return exportStringCache;
    }

    /**
     * Determines framework exports taking into account host components and package scanner configuration.
     *
     * @param regs                 The list of host component registrations
     * @param packageScannerConfig The configuration for the package scanning
     * @param cacheDir             No longer used. (method deprecated).
     * @return A list of exports, in a format compatible with OSGi headers
     * @deprecated Please use {@link #getExports}. Deprecated since 2.3.6
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public String determineExports(List<HostComponentRegistration> regs, PackageScannerConfiguration packageScannerConfig, File cacheDir) {
        return determineExports(regs, packageScannerConfig);
    }

    /**
     * Determines framework exports taking into account host components and package scanner configuration.
     *
     * @param regs                 The list of host component registrations
     * @param packageScannerConfig The configuration for the package scanning
     * @return A list of exports, in a format compatible with OSGi headers
     */
    String determineExports(List<HostComponentRegistration> regs, PackageScannerConfiguration packageScannerConfig) {

        String exports = null;

        StringBuilder origExports = new StringBuilder();
        origExports.append("org.osgi.framework; version=1.4.1,");
        origExports.append("org.osgi.service.packageadmin; version=1.2.0,");
        origExports.append("org.osgi.service.startlevel; version=1.1.0,");
        origExports.append("org.osgi.service.url; version=1.0.0,");
        origExports.append("org.osgi.util; version=1.4.1,");
        origExports.append("org.osgi.util.tracker; version=1.4.1,");
        origExports.append("host.service.command; version=1.0.0,");

        constructJdkExports(origExports, JDK_PACKAGES_PATH);
        origExports.append(",");

        if (System.getProperty("java.specification.version").equals("1.6")) {
            constructJdkExports(origExports, JDK6_PACKAGES_PATH);
            origExports.append(",");
        }

        Collection<ExportPackage> exportList = generateExports(packageScannerConfig);
        constructAutoExports(origExports, exportList);


        try {
            origExports.append(OsgiHeaderUtil.findReferredPackages(regs, packageScannerConfig.getPackageVersions()));

            Analyzer analyzer = new Analyzer();
            analyzer.setJar(new Jar("somename.jar"));

            // we pretend the exports are imports for the sake of the bnd tool, which would otherwise cut out
            // exports that weren't actually in the jar
            analyzer.setProperty(Constants.IMPORT_PACKAGE, origExports.toString());
            Manifest mf = analyzer.calcManifest();

            exports = mf.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
        } catch (IOException ex) {
            log.error("Unable to calculate necessary exports based on host components", ex);
            exports = origExports.toString();
        }

        if (log.isDebugEnabled()) {
            log.debug("Exports:\n" + exports.replaceAll(",", "\r\n"));
        }
        return exports;
    }

    void constructAutoExports(StringBuilder sb, Collection<ExportPackage> packageExports) {
        for (ExportPackage pkg : packageExports) {
            sb.append(pkg.getPackageName());
            if (pkg.getVersion() != null) {
                try {
                    Version.parseVersion(pkg.getVersion());
                    sb.append(";version=").append(pkg.getVersion());
                } catch (IllegalArgumentException ex) {
                    log.info("Unable to parse version: " + pkg.getVersion());
                }
            }
            sb.append(",");
        }
    }

    Collection<ExportPackage> generateExports(PackageScannerConfiguration packageScannerConfig) {
        String[] arrType = new String[0];

        Map<String, String> pkgVersions = new HashMap<String, String>(packageScannerConfig.getPackageVersions());
        if (packageScannerConfig.getServletContext() != null) {
            String ver = packageScannerConfig.getServletContext().getMajorVersion() + "." + packageScannerConfig.getServletContext().getMinorVersion();
            pkgVersions.put("javax.servlet*", ver);
        }

        PackageScanner scanner = new PackageScanner()
                .select(
                        jars(
                                include(packageScannerConfig.getJarIncludes().toArray(arrType)),
                                exclude(packageScannerConfig.getJarExcludes().toArray(arrType))),
                        packages(
                                include(packageScannerConfig.getPackageIncludes().toArray(arrType)),
                                exclude(packageScannerConfig.getPackageExcludes().toArray(arrType)))
                )
                .withMappings(pkgVersions);

        if (log.isDebugEnabled()) {
            scanner.enableDebug();
        }

        Collection<ExportPackage> exports = scanner.scan();
        log.info("Package scan completed. Found " + exports.size() + " packages to export.");

        if (!isPackageScanSuccessful(exports) && packageScannerConfig.getServletContext() != null) {
            log.warn("Unable to find expected packages via classloader scanning.  Trying ServletContext scanning...");
            ServletContext ctx = packageScannerConfig.getServletContext();
            try {
                exports = scanner.scan(ctx.getResource("/WEB-INF/lib"), ctx.getResource("/WEB-INF/classes"));
            }
            catch (MalformedURLException e) {
                log.warn("Unable to scan webapp for packages", e);
            }
        }

        if (!isPackageScanSuccessful(exports)) {
            throw new IllegalStateException("Unable to find required packages via classloader or servlet context"
                    + " scanning, most likely due to an application server bug.");
        }
        return exports;
    }

    /**
     * Tests to see if a scan of packages to export was successful, using the presence of slf4j as the criteria.
     *
     * @param exports The exports found so far
     * @return True if slf4j is present, false otherwise
     */
    private static boolean isPackageScanSuccessful(Collection<ExportPackage> exports) {
        boolean slf4jFound = false;
        for (ExportPackage export : exports) {
            if (export.getPackageName().equals("org.slf4j")) {
                slf4jFound = true;
                break;
            }
        }
        return slf4jFound;
    }

    void constructJdkExports(StringBuilder sb, String packageListPath) {
        InputStream in = null;
        try {
            in = ClassLoaderUtils.getResourceAsStream(packageListPath, ExportsBuilder.class);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    if (line.charAt(0) != '#') {
                        if (sb.length() > 0)
                            sb.append(',');
                        sb.append(line);
                    }
                }
            }
        } catch (IOException e) {
            IOUtils.closeQuietly(in);
        }
    }
}
