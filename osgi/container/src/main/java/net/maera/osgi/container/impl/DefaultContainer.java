package net.maera.osgi.container.impl;

import com.google.common.base.Strings;
import net.maera.io.Resource;
import net.maera.osgi.PackagesBuilder;
import net.maera.osgi.container.ContainerException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import java.util.Map;

/**
 * @author Les Hazlewood
 * @since 0.1
 */
public class DefaultContainer extends LifecycleContainer {

    protected static final String MAERA_PROPERTY_PREFIX = "maera.";

    protected static final String OSGI_BOOT_DELEGATION_PROPERTY = "org.osgi.framework.bootdelegation";
    
    private Map<String,String> bootDelegationPackages;
    private Map<String,String> extraBootDelegationPackages;
    private Map<String,String> extraSystemPackages;

    private HostActivator hostActivator;

    public DefaultContainer() {
        super();
        hostActivator = new DefaultHostActivator();
    }

    @Override
    public Bundle installBundle(Resource resource) throws ContainerException {
        if ( this.hostActivator == null) {
            throw new NullPointerException("HostActivator class attribute is null.  Subclass implementations " +
                    "must provide a HostActivator instance.");
        }
        try {
            return this.hostActivator.installBundle(resource, false);
        } catch (BundleException e) {
            throw new ContainerException("Unable to install bundle via HostActivator.", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    protected Map<String, Object> prepareFrameworkConfig(Map<String, Object> configMap) {

        // Add the bundle provided service interface package and the core OSGi
        // packages to be exported from the class path via the system bundle.
        PackagesBuilder builder = new PackagesBuilder().withJdkDefaults().withOsgiDefaults().append(extraSystemPackages);
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, builder.toString());

        builder = new PackagesBuilder();
        String bootDelegation = Strings.emptyToNull(getMaeraSpecificSystemProperty(OSGI_BOOT_DELEGATION_PROPERTY));
        if (bootDelegation == null) {
            builder.withBootDelegationDefaults();
        }
        builder.append(extraBootDelegationPackages);
        configMap.put(Constants.FRAMEWORK_BOOTDELEGATION, builder.toString());

        validateConfiguration(configMap);

        return configMap;
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

    private String getMaeraSpecificSystemProperty(final String originalSystemProperty) {
        return System.getProperty(MAERA_PROPERTY_PREFIX + originalSystemProperty);
    }

    /**
     * Validate caches based on the list of packages exported from the application.  If the list has changed, the cache
     * directories should be cleared.
     *
     * @param systemExports The value of system exports in the header
     */
    private void validateCaches(String systemExports) {
        @SuppressWarnings({"UnusedDeclaration"})
        String cacheKey = String.valueOf(systemExports.hashCode());
        /*persistentCache.validate(cacheKey);

        log.debug("Using Felix bundle cache directory :" + persistentCache.getOsgiBundleCache().getAbsolutePath());*/
    }

    /**
     * @param configMap The Felix configuration
     * @throws ContainerException If any validation fails
     */
    private void validateConfiguration(Map<String, Object> configMap) throws ContainerException {
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

    public Map<String, String> getBootDelegationPackages() {
        return bootDelegationPackages;
    }

    public void setBootDelegationPackages(Map<String, String> bootDelegationPackages) {
        this.bootDelegationPackages = bootDelegationPackages;
    }

    public Map<String, String> getExtraBootDelegationPackages() {
        return extraBootDelegationPackages;
    }

    public void setExtraBootDelegationPackages(Map<String, String> extraBootDelegationPackages) {
        this.extraBootDelegationPackages = extraBootDelegationPackages;
    }

    public Map<String, String> getExtraSystemPackages() {
        return extraSystemPackages;
    }

    public void setExtraSystemPackages(Map<String, String> extraSystemPackages) {
        this.extraSystemPackages = extraSystemPackages;
    }

    public HostActivator getHostActivator() {
        return hostActivator;
    }

    public void setHostActivator(HostActivator hostActivator) {
        this.hostActivator = hostActivator;
    }
}
