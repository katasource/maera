package org.maera.plugin.manager;

import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginInstaller;
import org.maera.plugin.RevertablePluginInstaller;

/**
 * Wraps a plugin installer as a {@link org.maera.plugin.RevertablePluginInstaller} that does nothing
 * in its implementation.
 *
 * @since 2.5.0
 */
class NoOpRevertablePluginInstaller implements RevertablePluginInstaller {
    private final PluginInstaller delegate;

    public NoOpRevertablePluginInstaller(PluginInstaller delegate) {
        this.delegate = delegate;
    }

    public void revertInstalledPlugin(String pluginKey) {
        // op-op
    }

    public void clearBackups() {
        // no-op
    }

    public void installPlugin(String key, PluginArtifact pluginArtifact) {
        delegate.installPlugin(key, pluginArtifact);
    }
}
