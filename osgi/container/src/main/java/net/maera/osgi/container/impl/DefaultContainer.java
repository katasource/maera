package net.maera.osgi.container.impl;

import com.google.common.base.Strings;
import net.maera.osgi.PackagesBuilder;
import net.maera.osgi.container.ContainerException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @since 0.1
 */
public class DefaultContainer extends LifecycleContainer {

    protected static final String OSGI_BOOT_DELEGATION_PROPERTY = "org.osgi.framework.bootdelegation";
    protected static final String MAERA_PROPERTY_PREFIX = "maera.";

    private static final transient Logger log = LoggerFactory.getLogger(DefaultContainer.class);

    public DefaultContainer() {
        super();
    }

    public DefaultContainer(String startupThreadFactoryName) {
        super(startupThreadFactoryName);
    }

    @SuppressWarnings({"unchecked"})
    protected Map<?,?> prepareFrameworkConfig(Map configMap) {

        // Add the bundle provided service interface package and the core OSGi
        // packages to be exported from the class path via the system bundle.
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
                new PackagesBuilder().withOsgiDefaults().withJdkDefaults().toString());

        String bootDelegation = Strings.emptyToNull(getMaeraSpecificSystemProperty(OSGI_BOOT_DELEGATION_PROPERTY));
        if (bootDelegation == null) {
            // These exist to work around JAXP problems.  Specifically, bundles that use static factories to create JAXP
            // instances will execute FactoryFinder with the CCL set to the bundle.  These delegations ensure the appropriate
            // implementation is found and loaded.
            bootDelegation = "weblogic,weblogic.*," +
                    "META-INF.services," +
                    "com.yourkit,com.yourkit.*," +
                    "com.jprofiler,com.jprofiler.*," +
                    "org.apache.xerces,org.apache.xerces.*," +
                    "org.apache.xalan,org.apache.xalan.*," +
                    "sun.*,com.sun.*," +
                    "com.icl.saxon";
        }

        configMap.put(Constants.FRAMEWORK_BOOTDELEGATION, bootDelegation);

        validateConfiguration(configMap);

        return configMap;
    }

    private String getMaeraSpecificSystemProperty(final String originalSystemProperty) {
        return System.getProperty(MAERA_PROPERTY_PREFIX + originalSystemProperty);
    }

    /**
     * @param configMap The Felix configuration
     * @throws ContainerException If any validation fails
     */
    private void validateConfiguration(Map configMap) throws ContainerException {
        String systemExports = (String) configMap.get(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        validateCaches(systemExports);
        detectIncorrectOsgiVersion();
        detectXercesOverride(systemExports);
    }

    /**
     * Detect when xerces has no version, most likely due to an installation of Tomcat where an old version of xerces
     * is installed into common/lib/endorsed in order to support Java 1.4.
     *
     * @param systemExports The system exports
     * @throws ContainerException If xerces has no version
     */
    void detectXercesOverride(String systemExports) throws ContainerException {
        int pos = systemExports.indexOf("org.apache.xerces.util");
        if (pos > -1) {
            if (pos == 0 || (pos > 0 && systemExports.charAt(pos - 1) == ',')) {
                pos += "org.apache.xerces.util".length();

                // only fail if no xerces found and xerces has no version
                if (pos >= systemExports.length() || ';' != systemExports.charAt(pos)) {
                    String msg = "Detected an incompatible version of Apache Xerces on the classpath.  " +
                            "If using Tomcat, you may have an old version of Xerces in " +
                            "$TOMCAT_HOME/common/lib/endorsed that will need to be removed.";
                    throw new ContainerException(msg);
                }
            }
        }
    }

    /**
     * Validate caches based on the list of packages exported from the application.  If the list has changed, the cache
     * directories should be cleared.
     *
     * @param systemExports The value of system exports in the header
     */
    private void validateCaches(String systemExports) {
        String cacheKey = String.valueOf(systemExports.hashCode());
        /*persistentCache.validate(cacheKey);

        log.debug("Using Felix bundle cache directory :" + persistentCache.getOsgiBundleCache().getAbsolutePath());*/
    }

    /**
     * Detects incorrect configuration of WebSphere 6.1 that leaks pre 4.2 OSGi jars into the application
     *
     * @throws ContainerException if a pre 4.2 version of an OSGi environment is discovered
     */
    private void detectIncorrectOsgiVersion() throws ContainerException {
        try {
            Bundle.class.getMethod("getVersion");
        } catch (final NoSuchMethodException e) {
            String msg = "Detected older OSGi version (4.1 or earlier) - 4.2 or later is required.  If using " +
                    "WebSphere 6.1, please enable application-first (parent-last) classloading and the " +
                    "'Single classloader for application' WAR classloader policy.";
            throw new ContainerException(msg, e);
        }
    }
}
