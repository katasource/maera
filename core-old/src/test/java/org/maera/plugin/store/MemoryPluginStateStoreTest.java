/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:47:36 PM
 */
package org.maera.plugin.store;

import org.junit.Test;
import org.maera.plugin.manager.DefaultPluginPersistentState;
import org.maera.plugin.manager.PluginPersistentState;
import org.maera.plugin.manager.store.MemoryPluginPersistentStateStore;

import static org.junit.Assert.assertEquals;

public class MemoryPluginStateStoreTest {

    @Test
    public void testStore() {
        final MemoryPluginPersistentStateStore store = new MemoryPluginPersistentStateStore();
        final PluginPersistentState state = new DefaultPluginPersistentState();
        store.save(state);
        assertEquals(state.getMap(), store.load().getMap());
    }
}