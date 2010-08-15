package org.maera.plugin.manager;

import com.google.common.collect.ImmutableList;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.PluginController;
import org.maera.plugin.PluginState;
import org.maera.plugin.util.PluginUtils;
import org.maera.plugin.util.WaitUntil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Helper class that handles the problem of enabling a set of plugins at once.  This functionality is used for both
 * the initial plugin loading and manual plugin enabling.  The system waits 60 seconds for all dependencies to be
 * resolved, then resets the timer to 5 seconds if only one remains.
 *
 * @since 2.2.0
 */
class PluginEnabler {
    private static final Logger log = LoggerFactory.getLogger(PluginEnabler.class);
    private static final long LAST_PLUGIN_TIMEOUT = 30 * 1000;
    private static final long LAST_PLUGIN_WARN_TIMEOUT = 5 * 1000;

    private final PluginAccessor pluginAccessor;
    private final PluginController pluginController;

    public PluginEnabler(PluginAccessor pluginAccessor, PluginController pluginController) {
        this.pluginAccessor = pluginAccessor;
        this.pluginController = pluginController;
    }

    /**
     * Determines, recursively, which disabled plugins this plugin depends upon, and enables all of them at once.
     * Returns the plugins that were successfully enabled, including dependencies that weren't explicitly specified.
     *
     * @param plugins The set of plugins to enable
     * @return a collection of plugins that were actually enabled
     */
    Collection<Plugin> enableAllRecursively(Collection<Plugin> plugins) {
        Collection<Plugin> pluginsToEnable = new ArrayList<Plugin>();
        Set<String> dependentKeys = new HashSet<String>();

        for (Plugin plugin : plugins) {
            scanDependencies(plugin, dependentKeys);
        }

        for (String key : dependentKeys) {
            pluginsToEnable.add(pluginAccessor.getPlugin(key));
        }
        enable(pluginsToEnable);

        ImmutableList.Builder enabledPlugins = new ImmutableList.Builder();
        for (Plugin plugin : pluginsToEnable) {
            if (plugin.getPluginState().equals(PluginState.ENABLED)) {
                enabledPlugins.add(plugin);
            }
        }
        return enabledPlugins.build();
    }

    /**
     * Enables a collection of plugins at once, waiting for 60 seconds.  If any plugins are still in the enabling state,
     * the plugins are explicitly disabled.
     *
     * @param plugins The plugins to enable
     */
    void enable(Collection<Plugin> plugins) {
        final Set<Plugin> pluginsInEnablingState = new HashSet<Plugin>();
        for (final Plugin plugin : plugins) {
            try {
                plugin.enable();
                if (plugin.getPluginState() == PluginState.ENABLING) {
                    pluginsInEnablingState.add(plugin);
                }
            }
            catch (final RuntimeException ex) {
                log.error("Unable to enable plugin " + plugin.getKey(), ex);
            }
        }

        if (!pluginsInEnablingState.isEmpty()) {
            // Now try to enable plugins that weren't enabled before, probably due to dependency ordering issues
            WaitUntil.invoke(new WaitUntil.WaitCondition() {
                private long singlePluginTimeout;
                private long singlePluginWarn;

                public boolean isFinished() {
                    if (singlePluginTimeout > 0 && singlePluginTimeout < System.currentTimeMillis()) {
                        return true;
                    }
                    for (final Iterator<Plugin> i = pluginsInEnablingState.iterator(); i.hasNext();) {
                        final Plugin plugin = i.next();
                        if (plugin.getPluginState() != PluginState.ENABLING) {
                            i.remove();
                        }
                    }
                    if (isAtlassianDevMode() && pluginsInEnablingState.size() == 1) {
                        final long currentTime = System.currentTimeMillis();
                        if (singlePluginTimeout == 0) {
                            log.info("Only one plugin left not enabled. Resetting the timeout to " +
                                    (LAST_PLUGIN_TIMEOUT / 1000) + " seconds.");

                            singlePluginWarn = currentTime + LAST_PLUGIN_WARN_TIMEOUT;
                            singlePluginTimeout = currentTime + LAST_PLUGIN_TIMEOUT;
                        } else if (singlePluginWarn <= currentTime) {
                            //PLUG-617: Warn people when it takes a long time to enable a plugin when in dev mode. We bumped
                            //this timeout from 5 to 30 seconds because the gadget publisher in JIRA can take this long to
                            //load when running java in DEBUG mode. We are also now going to log a message about slow startup
                            //since 30 seconds is a long time to wait for your plugin to fail.
                            final Plugin plugin = pluginsInEnablingState.iterator().next();
                            final long remainingWait = Math.max(0, Math.round((singlePluginTimeout - currentTime) / 1000.0));

                            log.warn("Plugin '" + plugin + "' did not enable within " + (LAST_PLUGIN_WARN_TIMEOUT / 1000) + " seconds."
                                    + "The plugin should not take this long to enable. Will only attempt to load plugin for another '"
                                    + remainingWait + "' seconds.");
                            singlePluginWarn = Long.MAX_VALUE;
                        }
                    }
                    return pluginsInEnablingState.isEmpty();
                }

                public String getWaitMessage() {
                    return "Plugins that have yet to be enabled: " + pluginsInEnablingState;
                }

                private boolean isAtlassianDevMode() {
                    return Boolean.getBoolean(PluginUtils.ATLASSIAN_DEV_MODE);
                }
            }, PluginUtils.getDefaultEnablingWaitPeriod(), TimeUnit.SECONDS, 1);

            // Disable any plugins that aren't enabled by now
            if (!pluginsInEnablingState.isEmpty()) {
                final StringBuilder sb = new StringBuilder();
                for (final Plugin plugin : pluginsInEnablingState) {
                    sb.append(plugin.getKey()).append(',');
                    pluginController.disablePluginWithoutPersisting(plugin.getKey());
                }
                sb.deleteCharAt(sb.length() - 1);
                log.error("Unable to start the following plugins due to timeout while waiting for plugin to enable: " + sb.toString());
            }
        }
    }

    /**
     * Scans, recursively, to build a set of plugin dependencies for the target plugin
     *
     * @param plugin        The plugin to scan
     * @param dependentKeys The set of keys collected so far
     */
    private void scanDependencies(Plugin plugin, Set<String> dependentKeys) {
        dependentKeys.add(plugin.getKey());

        // Ensure dependent plugins are enabled first
        for (String dependencyKey : plugin.getRequiredPlugins()) {
            if (!dependentKeys.contains(dependencyKey) &&
                    (pluginAccessor.getPlugin(dependencyKey) != null) &&
                    !pluginAccessor.isPluginEnabled(dependencyKey)) {
                scanDependencies(pluginAccessor.getPlugin(dependencyKey), dependentKeys);
            }
        }
    }
}
