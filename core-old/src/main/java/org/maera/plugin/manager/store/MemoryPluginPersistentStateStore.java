package org.maera.plugin.manager.store;

import org.maera.plugin.manager.DefaultPluginPersistentState;
import org.maera.plugin.manager.PluginPersistentState;
import org.maera.plugin.manager.PluginPersistentStateStore;

/**
 * A basic plugin state store that stores state in memory. Not recommended for production use.
 */
public class MemoryPluginPersistentStateStore implements PluginPersistentStateStore {
    private volatile PluginPersistentState state = new DefaultPluginPersistentState();

    public void save(final PluginPersistentState state) {
        this.state = state;
    }

    public PluginPersistentState load() {
        return state;
    }
}
