package org.maera.plugin.spring;

import org.maera.plugin.osgi.container.PackageScannerConfiguration;
import org.maera.plugin.osgi.container.impl.DefaultPackageScannerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.List;

/**
 * Spring {@link org.springframework.beans.factory.FactoryBean FactoryBean} implementation that creates a
 * {@link PackageScannerConfiguration} instance at startup based on Spring configuration.
 *
 * @since 0.1
 */
public class PackageScannerConfigurationFactoryBean extends AbstractFactoryBean/*<PackageScannerConfiguration>*/
        implements ServletContextAware {

    private static final transient Logger log = LoggerFactory.getLogger(PackageScannerConfigurationFactoryBean.class);

    private List<String> additionalPackageIncludes;
    private List<String> additionalPackageExcludes;
    private String applicationVersion;
    private List<String> packageIncludes;
    private List<String> packageExcludes;
    private ServletContext servletContext;

    public PackageScannerConfigurationFactoryBean() {
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public Class<?> getObjectType() {
        return PackageScannerConfiguration.class;
    }

    @Override
    protected PackageScannerConfiguration createInstance() throws Exception {
        DefaultPackageScannerConfiguration instance;
        if (StringUtils.hasText(this.applicationVersion)) {
            instance = new DefaultPackageScannerConfiguration(this.applicationVersion);
        } else {
            instance = new DefaultPackageScannerConfiguration();
        }

        if (servletContext != null) {
            log.trace("ServletContext was injected successfully (WebApplicationContext).");
            instance.setServletContext(servletContext);
        }

        if (!CollectionUtils.isEmpty(packageIncludes) ) {
            instance.setPackageIncludes(packageIncludes);
        }

        if (!CollectionUtils.isEmpty(packageExcludes) ) {
            instance.setPackageExcludes(packageExcludes);
        }

        if (!CollectionUtils.isEmpty(additionalPackageIncludes)) {
            instance.getPackageIncludes().addAll(additionalPackageIncludes);
        }

        if (!CollectionUtils.isEmpty(additionalPackageExcludes)) {
            instance.getPackageExcludes().addAll(additionalPackageExcludes);
        }

        return instance;
    }

    public List<String> getAdditionalPackageExcludes() {
        return additionalPackageExcludes;
    }

    public void setAdditionalPackageExcludes(List<String> additionalPackageExcludes) {
        this.additionalPackageExcludes = additionalPackageExcludes;
    }

    public List<String> getAdditionalPackageIncludes() {
        return additionalPackageIncludes;
    }

    public void setAdditionalPackageIncludes(List<String> additionalPackageIncludes) {
        this.additionalPackageIncludes = additionalPackageIncludes;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public List<String> getPackageExcludes() {
        return packageExcludes;
    }

    public void setPackageExcludes(List<String> packageExcludes) {
        this.packageExcludes = packageExcludes;
    }

    public List<String> getPackageIncludes() {
        return packageIncludes;
    }

    public void setPackageIncludes(List<String> packageIncludes) {
        this.packageIncludes = packageIncludes;
    }
}
