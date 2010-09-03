package net.maera.engine;

import net.maera.io.Resource;
import net.maera.plugin.Plugin;

import java.util.Collection;

/**
 * @author Les Hazlewood
 * @since 0.1
 */
public interface PluginManager {

    Plugin getPlugin(String id);
    Plugin getPlugin(String key, String version);

    Collection<Plugin> getPlugins();

    void installPlugin(Resource pluginResource);

    void uninstall(Plugin plugin);
}
