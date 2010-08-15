package org.maera.plugin.osgi.factory;

import org.apache.commons.lang.Validate;
import org.maera.plugin.*;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.impl.AbstractPlugin;
import org.maera.plugin.osgi.util.BundleClassLoaderAccessor;
import org.maera.plugin.util.resource.AlternativeDirectoryResourceLoader;
import org.osgi.framework.*;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Plugin that wraps an OSGi bundle that has no plugin descriptor.
 */
public class OsgiBundlePlugin extends AbstractPlugin {

    private final Bundle bundle;
    private final PluginInformation pluginInformation;
    private final Date dateLoaded;
    private final String key;
    private final ClassLoader bundleClassLoader;
    private final SynchronousBundleListener bundleStopListener;

    public OsgiBundlePlugin(final Bundle bundle, final String key, final PluginEventManager pluginEventManager) {
        bundleClassLoader = BundleClassLoaderAccessor.getClassLoader(bundle, new AlternativeDirectoryResourceLoader());
        Validate.notNull(bundle);
        this.bundle = bundle;
        // TODO: this should be done at a higher level than this to support start and stop
        bundleStopListener = new SynchronousBundleListener() {
            public void bundleChanged(final BundleEvent bundleEvent) {
                if (bundleEvent.getBundle() == bundle) {
                    if (bundleEvent.getType() == BundleEvent.STOPPING) {
                        setPluginState(PluginState.DISABLED);
                    }
                }
            }
        };
        pluginInformation = new PluginInformation();
        pluginInformation.setDescription((String) bundle.getHeaders().get(Constants.BUNDLE_DESCRIPTION));
        pluginInformation.setVersion((String) bundle.getHeaders().get(Constants.BUNDLE_VERSION));
        pluginInformation.setVendorName((String) bundle.getHeaders().get(Constants.BUNDLE_VENDOR));

        this.key = key;
        dateLoaded = new Date();
    }


    @Override
    public int getPluginsVersion() {
        return 2;
    }

    @Override
    public void setPluginsVersion(final int version) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public String getName() {
        return (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
    }

    @Override
    public void setName(final String name) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public void setI18nNameKey(final String i18nNameKey) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String aPackage) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public void addModuleDescriptor(final ModuleDescriptor<?> moduleDescriptor) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public Collection<ModuleDescriptor<?>> getModuleDescriptors() {
        return Collections.emptyList();
    }

    @Override
    public ModuleDescriptor<?> getModuleDescriptor(final String key) {
        return null;
    }

    @Override
    public <M> List<ModuleDescriptor<M>> getModuleDescriptorsByModuleClass(final Class<M> aClass) {
        return Collections.emptyList();
    }

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    public void setEnabledByDefault(final boolean enabledByDefault) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public PluginInformation getPluginInformation() {
        return pluginInformation;
    }

    @Override
    public void setPluginInformation(final PluginInformation pluginInformation) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public void setResources(final Resourced resources) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public boolean isSystemPlugin() {
        return false;
    }

    @Override
    public boolean containsSystemModule() {
        return false;
    }

    @Override
    public void setSystemPlugin(final boolean system) {
        throw new UnsupportedOperationException("Not available");
    }

    @Override
    public Date getDateLoaded() {
        return dateLoaded;
    }

    public boolean isUninstallable() {
        return true;
    }

    public boolean isDeleteable() {
        return true;
    }

    public boolean isDynamicallyLoaded() {
        return true;
    }


    @Override
    public List<ResourceDescriptor> getResourceDescriptors() {
        return Collections.emptyList();
    }

    @Override
    public List<ResourceDescriptor> getResourceDescriptors(final String type) {
        return Collections.emptyList();
    }

    @Override
    public ResourceLocation getResourceLocation(final String type, final String name) {
        return null;
    }

    @Override
    public ResourceDescriptor getResourceDescriptor(final String type, final String name) {
        return null;
    }

    public <T> Class<T> loadClass(final String clazz, final Class<?> callingClass) throws ClassNotFoundException {
        return BundleClassLoaderAccessor.loadClass(bundle, clazz);
    }

    public URL getResource(final String name) {
        return bundleClassLoader.getResource(name);
    }

    public InputStream getResourceAsStream(final String name) {
        return bundleClassLoader.getResourceAsStream(name);
    }

    @Override
    protected void uninstallInternal() {
        try {
            bundle.uninstall();
        }
        catch (final BundleException e) {
            throw new PluginException(e);
        }
    }

    @Override
    protected PluginState enableInternal() {
        try {
            bundle.start();
            bundle.getBundleContext().addBundleListener(bundleStopListener);
            return PluginState.ENABLED;
        }
        catch (final BundleException e) {
            throw new PluginException(e);
        }
    }

    @Override
    protected void disableInternal() {
        try {
            if (bundle.getState() == Bundle.ACTIVE) {
                bundle.stop();
            }
        }
        catch (final BundleException e) {
            throw new PluginException(e);
        }
    }

    public ClassLoader getClassLoader() {
        return bundleClassLoader;
    }

}
