package org.maera.plugin.manager;

import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginInstaller;

/**
 * Simple implementation of {@link PluginInstaller} that throws an exception if called
 *
 * @since 2.5.0
 */
class UnsupportedPluginInstaller implements PluginInstaller {
    public void installPlugin(String key, PluginArtifact pluginArtifact) {
        throw new UnsupportedOperationException("Dynamic plugin installation is not supported");
    }
}
