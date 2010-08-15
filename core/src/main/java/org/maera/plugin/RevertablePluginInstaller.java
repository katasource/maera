package org.maera.plugin;

/**
 * A type of PluginInstaller that supports reverting plugin artifacts installed during the current running.  Specifically,
 * it is used during upgrades of plugins that use modules that require a restart, and therefore, is of no use to environments
 * that have no such descriptors.
 * <p/>
 * Only plugins that were installed while the current instance has existed are eligible to be reverted.  In
 * other words, no state is persisted across restarts.
 *
 * @since 2.5.0
 */
public interface RevertablePluginInstaller extends PluginInstaller {
    /**
     * Reverts a plugin artifact that was installed to its original state.  If the artifact had already existed
     * before it was upgraded, the old artifact should be returned to its place.  If the artifact hadn't existed previously,
     * then the installed plugin artifact should just be deleted.  Calling this method after multiple installs or
     * upgrades for a given plugin key during an instance will simple restore the original artifact, if any.
     *
     * @param pluginKey The plugin key to revert
     */
    void revertInstalledPlugin(String pluginKey);

    /**
     * Clears any backed up artifacts from the previous run.
     */
    void clearBackups();
}
