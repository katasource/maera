package net.maera.plugin.mgt;

import net.maera.io.Resource;
import net.maera.plugin.Plugin;

import java.util.Collection;

/**
 * @author Les Hazlewood
 * @since 0.1
 */
public interface PluginManager {

    Plugin getPlugin(String key);

    Collection<Plugin> getPlugins();

    void installPlugins(Resource... pluginResource);

    void uninstall(Plugin... plugins);
}
