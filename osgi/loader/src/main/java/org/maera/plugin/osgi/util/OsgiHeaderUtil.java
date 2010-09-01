package org.maera.plugin.osgi.util;

import aQute.libg.header.OSGiHeader;
import org.apache.commons.io.IOUtils;
import org.maera.plugin.osgi.factory.OsgiPlugin;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.util.ClassLoaderUtils;
import org.maera.plugin.util.ClassUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * Utilities to help create OSGi headers
 *
 * @since 0.1
 */
public class OsgiHeaderUtil {

    static final String JDK_PACKAGES_PATH = "jdk-packages.txt";
    static final String JDK6_PACKAGES_PATH = "jdk6-packages.txt";
    static Logger log = LoggerFactory.getLogger(OsgiHeaderUtil.class);

    /**
     * Finds all referred packages for host component registrations by scanning their declared interfaces' bytecode.
     *
     * @param registrations A list of host component registrations
     * @return The referred packages in a format compatible with an OSGi header
     * @throws IOException If there are any problems scanning bytecode
     * @since 2.4.0
     */
    public static String findReferredPackages(List<HostComponentRegistration> registrations) throws IOException {
        return findReferredPackages(registrations, Collections.<String, String>emptyMap());
    }

    /**
     * Finds all referred packages for host component registrations by scanning their declared interfaces' bytecode.
     *
     * @param registrations A list of host component registrations
     * @return The referred packages in a format compatible with an OSGi header
     * @throws IOException If there are any problems scanning bytecode
     */
    public static String findReferredPackages(List<HostComponentRegistration> registrations, Map<String, String> packageVersions) throws IOException {
        StringBuffer sb = new StringBuffer();
        Set<String> referredPackages = new HashSet<String>();
        Set<String> referredClasses = new HashSet<String>();
        if (registrations == null) {
            sb.append(",");
        } else {
            for (HostComponentRegistration reg : registrations) {
                Set<Class> classesToScan = new HashSet<Class>();

                // Make sure we scan all extended interfaces as well
                for (Class inf : reg.getMainInterfaceClasses())
                    ClassUtils.findAllTypes(inf, classesToScan);

                for (Class inf : classesToScan) {
                    String clsName = inf.getName().replace('.', '/') + ".class";
                    crawlReferenceTree(clsName, referredClasses, referredPackages, 1);
                }
            }
            for (String pkg : referredPackages) {
                String version = packageVersions.get(pkg);
                sb.append(pkg);
                if (version != null) {
                    try {
                        Version.parseVersion(version);
                        sb.append(";version=").append(version);
                    } catch (IllegalArgumentException ex) {
                        log.info("Unable to parse version: " + version);
                    }
                }
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * This will crawl the class interfaces to the desired level.
     *
     * @param className      name of the class.
     * @param scannedClasses set of classes that have been scanned.
     * @param packageImports set of imports that have been found.
     * @param level          depth of scan (recursion).
     * @throws IOException error loading a class.
     */
    static void crawlReferenceTree(String className, Set<String> scannedClasses, Set<String> packageImports, int level) throws IOException {
        if (level <= 0) {
            return;
        }

        if (className.startsWith("java/"))
            return;

        if (scannedClasses.contains(className))
            return;
        else
            scannedClasses.add(className);

        if (log.isDebugEnabled())
            log.debug("Crawling " + className);

        InputStream in = null;
        try {
            in = ClassLoaderUtils.getResourceAsStream(className, OsgiHeaderUtil.class);

            if (in == null) {
                log.error("Cannot find interface " + className);
                return;
            }
            Clazz clz = new Clazz(className, in);
            packageImports.addAll(clz.getReferred().keySet());

            Set<String> referredClasses = clz.getReferredClasses();
            for (String ref : referredClasses)
                crawlReferenceTree(ref, scannedClasses, packageImports, level - 1);
        }
        finally {
            IOUtils.closeQuietly(in);
        }

    }

    /**
     * Parses an OSGi header line into a map structure
     *
     * @param header The header line
     * @return A map with the key the entry value and the value a map of attributes
     * @since 2.2.0
     */
    public static Map<String, Map<String, String>> parseHeader(String header) {
        return OSGiHeader.parseHeader(header);
    }

    /**
     * Builds the header string from a map
     *
     * @param key   The header value
     * @param attrs The map of attributes
     * @return A string, suitable for inclusion into an OSGI header string
     * @since 2.2.0
     */
    public static String buildHeader(String key, Map<String, String> attrs) {
        StringBuilder fullPkg = new StringBuilder(key);
        if (attrs != null && !attrs.isEmpty()) {
            for (Map.Entry<String, String> entry : attrs.entrySet()) {
                fullPkg.append(";");
                fullPkg.append(entry.getKey());
                fullPkg.append("=\"");
                fullPkg.append(entry.getValue());
                fullPkg.append("\"");
            }
        }
        return fullPkg.toString();
    }

    /**
     * Gets the plugin key from the bundle
     * <p/>
     * WARNING: shamelessly copied at {@link org.maera.plugin.osgi.bridge.PluginBundleUtils}, which can't use
     * this class due to creating a cyclic build dependency.  Ensure these two implementations are in sync.
     *
     * @param bundle The plugin bundle
     * @return The plugin key, cannot be null
     * @since 2.2.0
     */
    public static String getPluginKey(Bundle bundle) {
        return getPluginKey(
                bundle.getSymbolicName(),
                bundle.getHeaders().get(OsgiPlugin.MAERA_PLUGIN_KEY),
                bundle.getHeaders().get(Constants.BUNDLE_VERSION)
        );
    }

    /**
     * Gets the plugin key from the jar manifest
     *
     * @param mf The plugin jar manifest
     * @return The plugin key, cannot be null
     * @since 2.2.0
     */
    public static String getPluginKey(Manifest mf) {
        return getPluginKey(
                mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME),
                mf.getMainAttributes().getValue(OsgiPlugin.MAERA_PLUGIN_KEY),
                mf.getMainAttributes().getValue(Constants.BUNDLE_VERSION)
        );
    }

    private static String getPluginKey(Object bundleName, Object atlKey, Object version) {
        Object key = atlKey;
        if (key == null) {
            key = bundleName + "-" + version;
        }
        return key.toString();

    }
}
