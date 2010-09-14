package org.maera.plugin.loaders.classloading;

import org.maera.plugin.PluginException;

import java.util.Collection;

/**
 * Monitors some hypothetical space for deployed plugins. Due to limitations in the plugin system, plugins must
 * at some point be represented as files, so for situations where plugins are not files (i.e. database-stored
 * plugins) the scanner is responsible for copying them to the filesystem before they are used)
 *
 * @since 2.1.0
 */
public interface Scanner {
    /**
     * Scan for new deployment units. On the first scan, all deployment units that the scanner can find will
     * be returned. Subsequent scans will only return deployment units that are new since the last scan (or
     * call to reset() or clear())
     *
     * @return all new deployment units since the last scan
     */
    Collection<DeploymentUnit> scan();

    /**
     * Gets all deployment units currently being tracked by the scanner. This <i>will not</i> trigger
     * a scan, meaning that plugins that have been added since the last scan will not be returned.
     *
     * @return a collection of all deployment units currently being tracked by the scanner.
     */
    Collection<DeploymentUnit> getDeploymentUnits();

    /**
     * Reset the scanner. This causes it to forget all state about which plugins have (or haven't) been loaded.
     */
    void reset();

    /**
     * Remove the specified deployment unit in such a way as it will not be picked up by subsequent scans, even
     * if the system is restarted.
     *
     * @param unit the deployment unit to remove
     * @throws PluginException if the unit has not been properly removed: i.e. a restart would mean the unit would
     *                         be reloaded.
     */
    void remove(DeploymentUnit unit) throws PluginException;
}
