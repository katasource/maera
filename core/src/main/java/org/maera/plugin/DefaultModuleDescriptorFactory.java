package org.maera.plugin;

import com.atlassian.util.concurrent.CopyOnWriteMap;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.hostcontainer.HostContainer;
import org.maera.plugin.util.ClassLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static java.util.Collections.unmodifiableMap;

/**
 * Default implementation of a descriptor factory that allows filtering of
 * descriptor keys
 */
public class DefaultModuleDescriptorFactory implements ModuleDescriptorFactory {
    private static Logger log = LoggerFactory.getLogger(DefaultModuleDescriptorFactory.class);

    @SuppressWarnings("unchecked")
    private final Map<String, Class<? extends ModuleDescriptor>> moduleDescriptorClasses = CopyOnWriteMap.<String, Class<? extends ModuleDescriptor>>builder().stableViews()
            .newHashMap();
    private final List<String> permittedModuleKeys = new ArrayList<String>();
    private final HostContainer hostContainer;

    /**
     * @deprecated Since 2.2.0, use
     *             {@link #DefaultModuleDescriptorFactory(HostContainer)}
     *             instead
     */
    @Deprecated
    public DefaultModuleDescriptorFactory() {
        this(new DefaultHostContainer());
    }

    /**
     * Instantiates a descriptor factory that uses the host container to create
     * descriptors
     *
     * @param hostContainer The host container implementation for descriptor
     *                      creation
     * @since 2.2.0
     */
    public DefaultModuleDescriptorFactory(final HostContainer hostContainer) {
        this.hostContainer = hostContainer;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends ModuleDescriptor> getModuleDescriptorClass(final String type) {
        return moduleDescriptorClasses.get(type);
    }

    public ModuleDescriptor<?> getModuleDescriptor(final String type) throws PluginParseException, IllegalAccessException, InstantiationException, ClassNotFoundException {
        if (shouldSkipModuleOfType(type)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final Class<? extends ModuleDescriptor> moduleDescriptorClazz = getModuleDescriptorClass(type);

        if (moduleDescriptorClazz == null) {
            throw new PluginParseException("Cannot find ModuleDescriptor class for plugin of type '" + type + "'.");
        }

        return hostContainer.create(moduleDescriptorClazz);
    }

    protected boolean shouldSkipModuleOfType(final String type) {
        synchronized (permittedModuleKeys) {
            return (permittedModuleKeys != null) && !permittedModuleKeys.isEmpty() && !permittedModuleKeys.contains(type);
        }
    }

    public void setModuleDescriptors(final Map<String, String> moduleDescriptorClassNames) {
        for (final Entry<String, String> entry : moduleDescriptorClassNames.entrySet()) {
            final Class<? extends ModuleDescriptor<?>> descriptorClass = getClassFromEntry(entry);
            if (descriptorClass != null) {
                addModuleDescriptor(entry.getKey(), descriptorClass);
            }
        }
    }

    private <D extends ModuleDescriptor<?>> Class<D> getClassFromEntry(final Map.Entry<String, String> entry) {
        if (shouldSkipModuleOfType(entry.getKey())) {
            return null;
        }

        try {
            final Class<D> descriptorClass = ClassLoaderUtils.<D>loadClass(entry.getValue(), getClass());

            if (!ModuleDescriptor.class.isAssignableFrom(descriptorClass)) {
                log.error("Configured plugin module descriptor class " + entry.getValue() + " does not inherit from ModuleDescriptor");
                return null;
            }
            return descriptorClass;
        }
        catch (final ClassNotFoundException e) {
            log.error("Unable to add configured plugin module descriptor " + entry.getKey() + ". Class not found: " + entry.getValue());
            return null;
        }
    }

    public boolean hasModuleDescriptor(final String type) {
        return moduleDescriptorClasses.containsKey(type);
    }

    @SuppressWarnings("unchecked")
    public void addModuleDescriptor(final String type, final Class<? extends ModuleDescriptor> moduleDescriptorClass) {
        moduleDescriptorClasses.put(type, moduleDescriptorClass);
    }

    public void removeModuleDescriptorForType(final String type) {
        moduleDescriptorClasses.remove(type);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Class<? extends ModuleDescriptor>> getDescriptorClassesMap() {
        return unmodifiableMap(moduleDescriptorClasses);
    }

    /**
     * Sets the list of module keys that will be loaded. If this list is empty,
     * then the factory will permit all recognised module types to load. This
     * allows you to run the plugin system in a 'restricted mode'
     *
     * @param permittedModuleKeys List of (String) keys
     */
    public void setPermittedModuleKeys(List<String> permittedModuleKeys) {
        if (permittedModuleKeys == null) {
            permittedModuleKeys = Collections.emptyList();
        }

        synchronized (this.permittedModuleKeys) {
            // synced
            this.permittedModuleKeys.clear();
            this.permittedModuleKeys.addAll(permittedModuleKeys);
        }
    }
}
