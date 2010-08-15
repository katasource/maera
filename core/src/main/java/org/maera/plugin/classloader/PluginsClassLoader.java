package org.maera.plugin.classloader;

import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.event.PluginEventListener;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.event.events.PluginEnabledEvent;
import org.maera.plugin.event.events.PluginModuleEnabledEvent;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

import static org.maera.plugin.util.Assertions.notNull;

/**
 * A ClassLoader that will loop over all enabled Plugins, attempting to load the given class (or other resource) from
 * the ClassLoader of each plugin in turn.
 *
 * @see org.maera.plugin.classloader.PluginClassLoader
 */
public class PluginsClassLoader extends AbstractClassLoader {
    private static final Logger log = LoggerFactory.getLogger(PluginsClassLoader.class);

    private final PluginAccessor pluginAccessor;

    private final Map<String, Plugin> pluginResourceIndex = new HashMap<String, Plugin>();
    private final Map<String, Plugin> pluginClassIndex = new HashMap<String, Plugin>();

    private final Set<String> missedPluginResource = new HashSet<String>();
    private final Set<String> missedPluginClass = new HashSet<String>();
    private ClassLoader parentClassLoader;

    public PluginsClassLoader(final PluginAccessor pluginAccessor) {
        this(null, pluginAccessor, new DefaultPluginEventManager());
    }

    /**
     * @deprecated Since 2.5.0, use {@link #PluginsClassLoader(ClassLoader, PluginAccessor, PluginEventManager)} instead
     */
    public PluginsClassLoader(final ClassLoader parent, final PluginAccessor pluginAccessor) {
        this(parent, pluginAccessor, new DefaultPluginEventManager());
    }

    /**
     * @param parent             The parent classloader
     * @param pluginAccessor     The plugin accessor
     * @param pluginEventManager The plugin event manager
     * @since 2.5.0
     */
    public PluginsClassLoader(final ClassLoader parent, final PluginAccessor pluginAccessor, PluginEventManager pluginEventManager) {
        super(parent);
        this.parentClassLoader = parent;
        this.pluginAccessor = notNull("pluginAccessor", pluginAccessor);
        pluginEventManager.register(this);
    }

    @Override
    protected URL findResource(final String name) {
        final Plugin indexedPlugin;
        synchronized (this) {
            indexedPlugin = pluginResourceIndex.get(name);
        }
        final URL result;
        if (isPluginEnabled(indexedPlugin)) {
            result = indexedPlugin.getClassLoader().getResource(name);
        } else {
            result = getResourceFromPlugins(name);
        }
        if (log.isDebugEnabled()) {
            log.debug("Find resource [ " + name + " ], found [ " + result + " ]");
        }
        return result;
    }

    @Override
    protected Class<?> findClass(final String className) throws ClassNotFoundException {
        final Plugin indexedPlugin;
        synchronized (this) {
            indexedPlugin = pluginClassIndex.get(className);
        }

        final Class<?> result;
        if (isPluginEnabled(indexedPlugin)) {
            result = indexedPlugin.getClassLoader().loadClass(className);
        } else {
            result = loadClassFromPlugins(className);
        }
        if (log.isDebugEnabled()) {
            log.debug("Find class [ " + className + " ], found [ " + result + " ]");
        }
        if (result != null) {
            return result;
        } else {
            throw new ClassNotFoundException(className);
        }
    }

    private Class<?> loadClassFromPlugins(final String className) {
        final boolean isMissedClassName;
        synchronized (this) {
            isMissedClassName = missedPluginClass.contains(className);
        }
        if (isMissedClassName) {
            return null;
        }
        final Collection<Plugin> plugins = pluginAccessor.getEnabledPlugins();
        if (log.isDebugEnabled()) {
            log.debug("loadClassFromPlugins (" + className + ") looping through plugins...");
        }
        for (final Plugin plugin : plugins) {
            if (log.isDebugEnabled()) {
                log.debug("loadClassFromPlugins (" + className + ") looking in plugin '" + plugin.getKey() + "'.");
            }
            try {
                final Class<?> result = plugin.getClassLoader().loadClass(className);
                //loadClass should never return null
                synchronized (this) {
                    pluginClassIndex.put(className, plugin);
                }
                if (log.isDebugEnabled()) {
                    log.debug("loadClassFromPlugins (" + className + ") found in plugin '" + plugin.getKey() + "'.");
                }
                return result;
            }
            catch (final ClassNotFoundException e) {
                // continue searching the other plugins
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("loadClassFromPlugins (" + className + ") not found - caching the miss.");
        }
        synchronized (this) {
            missedPluginClass.add(className);
        }
        return null;
    }

    private URL getResourceFromPlugins(final String name) {
        final boolean isMissedResource;
        synchronized (this) {
            isMissedResource = missedPluginResource.contains(name);
        }
        if (isMissedResource) {
            return null;
        }
        final Collection<Plugin> plugins = pluginAccessor.getEnabledPlugins();
        for (final Plugin plugin : plugins) {
            final URL resource = plugin.getClassLoader().getResource(name);
            if (resource != null) {
                synchronized (this) {
                    pluginResourceIndex.put(name, plugin);
                }
                return resource;
            }
        }
        synchronized (this) {
            missedPluginResource.add(name);
        }
        return null;
    }

    private boolean isPluginEnabled(final Plugin plugin) {
        return (plugin != null) && pluginAccessor.isPluginEnabled(plugin.getKey());
    }

    public synchronized void notifyUninstallPlugin(final Plugin plugin) {
        flushMissesCaches();
        for (final Iterator<Map.Entry<String, Plugin>> it = pluginResourceIndex.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, Plugin> resourceEntry = it.next();
            final Plugin pluginForResource = resourceEntry.getValue();
            if (plugin.getKey().equals(pluginForResource.getKey())) {
                it.remove();
            }
        }
        for (final Iterator<Map.Entry<String, Plugin>> it = pluginClassIndex.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, Plugin> pluginClassEntry = it.next();
            final Plugin pluginForClass = pluginClassEntry.getValue();
            if (plugin.getKey().equals(pluginForClass.getKey())) {
                it.remove();
            }
        }
    }

    /**
     * Returns the Plugin that will be used to load the given class name.
     * <p/>
     * If no enabled plugin can load the given class, then null is returned.
     *
     * @param className the Class name
     * @return the Plugin that will be used to load the given class name.
     * @since 2.3
     */
    public Plugin getPluginForClass(String className) {
        Plugin indexedPlugin;
        synchronized (this) {
            indexedPlugin = pluginClassIndex.get(className);
        }

        if (isPluginEnabled(indexedPlugin)) {
            return indexedPlugin;
        }
        // Don't let a plugin claim a class that is a system class.
        if (isSystemClass(className)) {
            return null;
        }
        // Plugin not indexed, or disabled
        // Try to load the class - this will cache the plugin it came from.
        Class clazz = loadClassFromPlugins(className);
        if (clazz == null) {
            // Class could not be loaded - so return null.
            return null;
        }
        synchronized (this) {
            // if we get here, then loadClassFromPlugins() has returned a non-null class, and the side effect is that
            // the plugin for the class name is cached in pluginClassIndex.
            indexedPlugin = pluginClassIndex.get(className);
        }
        return indexedPlugin;
    }

    private boolean isSystemClass(final String className) {
        try {
            // Assume that we are loaded by the core classloader
            getClass().getClassLoader().loadClass(className);
            // Standard ClassLoader was able to load the class
            return true;
        }
        catch (ClassNotFoundException ex) {
            // Look in the parentClassLoader (if any) - when is this actually used anyway? DefaultPluginManager sets it to null.
            if (parentClassLoader != null) {
                try {
                    parentClassLoader.loadClass(className);
                    // parentClassLoader was able to load the class
                    return true;
                }
                catch (ClassNotFoundException ex2) {
                    return false;
                }
            } else {
                // parentClassLoader == null. This is normal. 
                return false;
            }
        }
    }

    @PluginEventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        notifyPluginOrModuleEnabled();
    }

    @PluginEventListener
    public void onPluginModuleEnabled(PluginModuleEnabledEvent event) {
        notifyPluginOrModuleEnabled();
    }

    public synchronized void notifyPluginOrModuleEnabled() {
        flushMissesCaches();
    }

    private void flushMissesCaches() {
        missedPluginClass.clear();
        missedPluginResource.clear();
    }
}
