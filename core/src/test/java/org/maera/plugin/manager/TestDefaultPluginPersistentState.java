package org.maera.plugin.manager;

import junit.framework.TestCase;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.PluginRestartState;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.impl.StaticPlugin;

import java.util.Map;

public class TestDefaultPluginPersistentState extends TestCase {
    public void testSetEnabledPlugin() {
        PluginPersistentState state = new DefaultPluginPersistentState();
        final StaticPlugin plugin = createMockPlugin("mock.plugin.key", true);
        state = PluginPersistentState.Builder.create(state).setEnabled(plugin, true).toState();
        assertTrue(state.isEnabled(plugin));
        state = PluginPersistentState.Builder.create(state).setEnabled(plugin, false).toState();
        assertFalse(state.isEnabled(plugin));
    }

    public void testSetEnabledModuleDescriptor() {
        PluginPersistentState state = new DefaultPluginPersistentState();
        final ModuleDescriptor<?> module = createModule("mock.plugin.key", "module.key");
        state = PluginPersistentState.Builder.create(state).setEnabled(module, true).toState();
        assertTrue(state.isEnabled(module));
        state = PluginPersistentState.Builder.create(state).setEnabled(module, false).toState();
        assertFalse(state.isEnabled(module));
    }

    public void testGetPluginStateMap() {
        final StaticPlugin plugin1 = createMockPlugin("mock.plugin.key", true);
        final StaticPlugin plugin2 = createMockPlugin("two.mock.plugin.key", true);
        final ModuleDescriptor<?> module1 = createModule("mock.plugin.key", "module.key.1");
        final ModuleDescriptor<?> module2 = createModule("mock.plugin.key", "module.key.2");
        final ModuleDescriptor<?> module3 = createModule("mock.plugin.key", "module.key.3");
        // because all plugins and modules are enabled by default lets disable them

        final PluginPersistentState.Builder builder = PluginPersistentState.Builder.create();
        builder.setEnabled(plugin1, !plugin1.isEnabledByDefault());
        builder.setEnabled(plugin2, !plugin1.isEnabledByDefault());
        builder.setEnabled(module1, !module1.isEnabledByDefault());
        builder.setEnabled(module2, !module2.isEnabledByDefault());
        builder.setEnabled(module3, !module3.isEnabledByDefault());

        final PluginPersistentState state = builder.toState();
        final Map<String, Boolean> pluginStateMap = state.getPluginStateMap(plugin1);
        final PluginPersistentState pluginState = new DefaultPluginPersistentState(pluginStateMap);

        assertFalse(pluginState.isEnabled(plugin1) == plugin1.isEnabledByDefault());
        assertFalse(pluginState.isEnabled(module1) == module1.isEnabledByDefault());
        assertFalse(pluginState.isEnabled(module2) == module2.isEnabledByDefault());
        assertFalse(pluginState.isEnabled(module3) == module3.isEnabledByDefault());
        // plugin2 should not be part of the map therefore it should have default enabled value
        assertTrue(pluginState.isEnabled(plugin2) == plugin2.isEnabledByDefault());
    }

    public void testDefaultModuleStateIsNotStored() {
        final String pluginKey = "mock.plugin.key";
        StaticPlugin plugin = createMockPlugin(pluginKey, true);
        final PluginPersistentState.Builder builder = PluginPersistentState.Builder.create();
        builder.setEnabled(plugin, true);
        Map<String, Boolean> pluginStateMap = builder.toState().getPluginStateMap(plugin);
        assertTrue(pluginStateMap.isEmpty());

        builder.setEnabled(plugin, false);
        pluginStateMap = builder.toState().getPluginStateMap(plugin);
        assertFalse(pluginStateMap.isEmpty());

        builder.removeState(pluginKey);

        plugin = createMockPlugin(pluginKey, false);
        builder.setEnabled(plugin, false);
        pluginStateMap = builder.toState().getPluginStateMap(plugin);
        assertTrue(pluginStateMap.isEmpty());
        builder.setEnabled(plugin, true);
        pluginStateMap = builder.toState().getPluginStateMap(plugin);
        assertFalse(pluginStateMap.isEmpty());
    }

    public void testPluginRestartState() {
        PluginPersistentState state = new DefaultPluginPersistentState();
        assertEquals(PluginRestartState.NONE, state.getPluginRestartState("foo"));

        state = PluginPersistentState.Builder.create(state).setPluginRestartState("foo", PluginRestartState.REMOVE).toState();
        assertEquals(PluginRestartState.REMOVE, state.getPluginRestartState("foo"));

        PluginPersistentState stateCopy = new DefaultPluginPersistentState(state.getMap());
        assertEquals(PluginRestartState.REMOVE, stateCopy.getPluginRestartState("foo"));
        stateCopy = PluginPersistentState.Builder.create(state).clearPluginRestartState().toState();
        assertEquals(PluginRestartState.NONE, stateCopy.getPluginRestartState("foo"));
        assertEquals(PluginRestartState.REMOVE, state.getPluginRestartState("foo"));
    }

    public void testPluginRestartStateRemoveExisting() {
        final PluginPersistentState.Builder builder = PluginPersistentState.Builder.create();
        builder.setPluginRestartState("foo", PluginRestartState.UPGRADE);
        assertEquals(PluginRestartState.UPGRADE, builder.toState().getPluginRestartState("foo"));
        builder.setPluginRestartState("foo", PluginRestartState.REMOVE);
        assertEquals(PluginRestartState.REMOVE, builder.toState().getPluginRestartState("foo"));
    }

    private <T> ModuleDescriptor<T> createModule(final String pluginKey, final String moduleKey) {
        return new AbstractModuleDescriptor<T>() {
            @Override
            public T getModule() {
                return null;
            }

            @Override
            public String getCompleteKey() {
                return pluginKey + ':' + moduleKey;
            }
        };
    }

    private StaticPlugin createMockPlugin(final String pluginKey, final boolean enabledByDefault) {
        final StaticPlugin plugin = new StaticPlugin();
        plugin.setKey(pluginKey);
        plugin.setEnabledByDefault(enabledByDefault);
        return plugin;
    }

}
