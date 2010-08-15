package org.maera.plugin.loaders;

import org.maera.plugin.Plugin;
import org.maera.plugin.event.PluginEventManager;
import org.maera.plugin.factories.PluginFactory;
import org.maera.plugin.impl.AbstractDelegatingPlugin;
import org.maera.plugin.util.FileUtils;

import java.io.File;
import java.net.URL;
import java.util.List;

/**
 * Plugin loader that unzips plugins from a zip file into a local directory, and ensures that directory only contains
 * plugins from that zip file.  It also treats all plugins loaded from the directory as bundled plugins, meaning they
 * can can be upgraded, but not deleted.
 */
public class BundledPluginLoader extends DirectoryPluginLoader {
    public BundledPluginLoader(final URL zipUrl, final File pluginPath, final List<PluginFactory> pluginFactories, final PluginEventManager eventManager) {
        super(pluginPath, pluginFactories, eventManager);
        if (zipUrl == null) {
            throw new IllegalArgumentException("Bundled zip url cannot be null");
        }
        FileUtils.conditionallyExtractZipFile(zipUrl, pluginPath);
    }

    @Override
    protected Plugin postProcess(final Plugin plugin) {
        return new BundledPluginDelegate(plugin);
    }

    /**
     * Delegate that overrides methods to enforce bundled plugin behavior
     *
     * @Since 2.2.0
     */
    private static class BundledPluginDelegate extends AbstractDelegatingPlugin {

        public BundledPluginDelegate(Plugin delegate) {
            super(delegate);
        }

        @Override
        public boolean isBundledPlugin() {
            return true;
        }

        @Override
        public boolean isDeleteable() {
            return false;
        }
    }
}
