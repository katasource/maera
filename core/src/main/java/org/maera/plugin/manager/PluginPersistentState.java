package org.maera.plugin.manager;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginRestartState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.maera.plugin.manager.PluginPersistentState.Util.RESTART_STATE_SEPARATOR;
import static org.maera.plugin.manager.PluginPersistentState.Util.buildStateKey;

/**
 * Interface that represents a configuration state for plugins and plugin modules. The configuration state (enabled
 * or disabled) is separate from the plugins and modules themselves because a plugin may have multiple
 * states depending on the context.
 *
 * @author anatoli
 * @since 2.2.0
 */
public interface PluginPersistentState {
    /**
     * Get the map of all states.
     *
     * @return The map that maps plugins and modules' keys to a state (Boolean.True/Boolean.False). State stored in this map represents only
     *         the <i>differences</i> between the current state and the default state configured in the plugin(module).
     */
    Map<String, Boolean> getMap();

    /**
     * Whether or not a plugin is enabled, calculated from it's current state AND default state.
     */
    boolean isEnabled(final Plugin plugin);

    /**
     * Whether or not a given plugin module is enabled in this state, calculated from it's current state AND default state.
     */
    boolean isEnabled(final ModuleDescriptor<?> pluginModule);

    /**
     * Get state map of the given plugin and its modules
     *
     * @param plugin
     * @return The map that maps the plugin and its modules' keys to plugin state (Boolean.TRUE/Boolean.FALSE). State stored in this map represents only
     *         the <i>differences</i> between the current state and the default state configured in the plugin(module).
     */
    Map<String, Boolean> getPluginStateMap(final Plugin plugin);

    /**
     * Gets whether the plugin is expected to be upgraded, installed, or removed on next restart
     *
     * @param pluginKey The plugin to query
     * @return The state of the plugin on restart
     */
    PluginRestartState getPluginRestartState(String pluginKey);

    /**
     * Builder for {@link PluginPersistentState} instances.
     * <p/>
     * This class is <strong>not thread safe</strong>. It should
     * only be used in a method local context.
     *
     * @since 2.3.0
     */
    public static final class Builder {
        public static Builder create() {
            return new Builder();
        }

        public static Builder create(final PluginPersistentState state) {
            return new Builder(state);
        }

        private final Map<String, Boolean> map = new HashMap<String, Boolean>();

        Builder() {
        }

        Builder(final PluginPersistentState state) {
            map.putAll(state.getMap());
        }

        public PluginPersistentState toState() {
            return new DefaultPluginPersistentState(map, true);
        }

        public Builder setEnabled(final ModuleDescriptor<?> pluginModule, final boolean isEnabled) {
            setEnabled(pluginModule.getCompleteKey(), pluginModule.isEnabledByDefault(), isEnabled);
            return this;
        }

        public Builder setEnabled(final Plugin plugin, final boolean isEnabled) {
            setEnabled(plugin.getKey(), plugin.isEnabledByDefault(), isEnabled);
            return this;
        }

        private Builder setEnabled(final String completeKey, final boolean enabledByDefault, final boolean isEnabled) {
            if (isEnabled == enabledByDefault) {
                map.remove(completeKey);
            } else {
                map.put(completeKey, isEnabled);
            }
            return this;
        }

        /**
         * reset all plugin's state.
         */
        public Builder setState(final PluginPersistentState state) {
            map.clear();
            map.putAll(state.getMap());
            return this;
        }

        /**
         * Add the plugin state.
         */
        public Builder addState(final Map<String, Boolean> state) {
            map.putAll(state);
            return this;
        }

        /**
         * Remove a plugin's state.
         */
        public Builder removeState(final String key) {
            map.remove(key);
            return this;
        }

        public Builder setPluginRestartState(final String pluginKey, final PluginRestartState state) {
            // Remove existing state, if any
            for (final PluginRestartState st : PluginRestartState.values()) {
                map.remove(buildStateKey(pluginKey, st));
            }

            if (state != PluginRestartState.NONE) {
                map.put(buildStateKey(pluginKey, state), true);
            }
            return this;
        }

        public Builder clearPluginRestartState() {
            final Set<String> keys = new HashSet<String>(map.keySet());
            for (final String key : keys) {
                if (key.contains(RESTART_STATE_SEPARATOR)) {
                    map.remove(key);
                }
            }
            return this;
        }
    }

    static class Util {
        static final String RESTART_STATE_SEPARATOR = "--";

        static String buildStateKey(final String pluginKey, final PluginRestartState state) {
            final StringBuilder sb = new StringBuilder();
            sb.append(state.name());
            sb.append(RESTART_STATE_SEPARATOR);
            sb.append(pluginKey);
            return sb.toString();
        }
    }
}