package org.maera.plugin;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.impl.StaticPlugin;
import org.maera.plugin.loaders.PluginLoader;
import org.maera.plugin.manager.store.MemoryPluginPersistentStateStore;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Mockito.*;

/**
 * Tests that the plugin manager properly notifies StateAware plugin modules of state
 * transitions.
 */
public class StateAwareTest {

    private org.maera.plugin.manager.DefaultPluginManager manager;
    private Combination mockDisabled;
    private Combination mockEnabling;
    private ModuleDescriptor mockThwarted;
    private Plugin plugin1;

    @Before
    public void setUp() throws Exception {
        // FIXME - the next line is here to prevent a null pointer exception caused by a debug logging
        // a variable in the lifecycle is not initialized, which is fine for testing, but a debug logging causes an NPE

        Logger.getRootLogger().setLevel(Level.INFO);
        mockEnabling = makeMockModule(Combination.class, "key1", "enabling", true);
        mockDisabled = makeMockModule(Combination.class, "key1", "disabled", false);
        mockThwarted = makeMockModule(ModuleDescriptor.class, "key1", "thwarted", true);

        plugin1 = new StaticPlugin();
        plugin1.setPluginInformation(new PluginInformation());
        plugin1.setKey("key1");
        plugin1.enable();

        PluginLoader pluginLoader = setupPluginLoader(plugin1);
        ArrayList<PluginLoader> pluginLoaders = new ArrayList<PluginLoader>();
        pluginLoaders.add(pluginLoader);

        ModuleDescriptorFactory moduleDescriptorFactory = mock(ModuleDescriptorFactory.class);

        manager = new org.maera.plugin.manager.DefaultPluginManager(new MemoryPluginPersistentStateStore(), pluginLoaders, moduleDescriptorFactory, new DefaultPluginEventManager());

    }

    /**
     * If a plugin is enabled, any modules that are currently enabled should be sent the enabled
     * message, but modules which are disabled should not.
     */
    @Test
    public void testDisabledModuleDescriptorsAreEnabled() {
        plugin1.addModuleDescriptor(mockEnabling);
        plugin1.addModuleDescriptor(mockDisabled);
        plugin1.setEnabledByDefault(false);

        manager.init();

        manager.enablePlugin(plugin1.getKey());
        verify(mockEnabling).enabled();
    }

    /**
     * Any StateAware plugin module that is active when the plugin manager is initialised should
     * receive an enabled message
     */
    @Test
    public void testStateAwareOnInit() {
        plugin1.addModuleDescriptor(mockEnabling);
        plugin1.addModuleDescriptor(mockThwarted);
        plugin1.addModuleDescriptor(mockDisabled);
        manager.init();
        verify(mockEnabling).enabled();
    }

    /**
     * If a plugin is disabled, any modules that are currently enabled should be sent the disabled
     * message
     */
    @Test
    public void testStateAwareOnPluginDisable() {
        plugin1.addModuleDescriptor(mockEnabling);
        plugin1.addModuleDescriptor(mockDisabled);

        manager.init();
        verify(mockEnabling).enabled();

        manager.disablePlugin(plugin1.getKey());
        verify(mockEnabling).disabled();
    }

    /**
     * Any StateAware plugin moudle that is explicitly enabled or disabled through the plugin manager
     * should receive the appropriate message
     */
    @Test
    public void testStateAwareOnPluginModule() {
        plugin1.addModuleDescriptor(mockDisabled);
        manager.init();

        when(mockDisabled.satisfiesMinJavaVersion()).thenReturn(true);
        manager.enablePluginModule(mockDisabled.getCompleteKey());
        verify(mockDisabled).enabled();

        manager.disablePluginModule(mockDisabled.getCompleteKey());
        verify(mockDisabled).disabled();
    }

    private <T extends ModuleDescriptor> T makeMockModule(Class<T> moduleClass, String pluginKey, String moduleKey, boolean enabledByDefault) {
        T mock = Mockito.mock(moduleClass);
        when(mock.getKey()).thenReturn(moduleKey);
        when(mock.getCompleteKey()).thenReturn(pluginKey + ":" + moduleKey);
        when(mock.isEnabledByDefault()).thenReturn(enabledByDefault);
        return mock;
    }

    private PluginLoader setupPluginLoader(final Plugin plugin1) {
        return new PluginLoader() { //TODO: should this deployer support removal and addition?

            public Collection<Plugin> loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
                ArrayList<Plugin> list = new ArrayList<Plugin>();
                list.add(plugin1);
                return list;
            }

            public boolean supportsAddition() {
                return false;
            }

            public boolean supportsRemoval() {
                return false;
            }

            public Collection removeMissingPlugins() {
                return null;
            }

            public Collection<Plugin> addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory) {
                return null;
            }

            public void removePlugin(Plugin plugin) throws PluginException {
                throw new PluginException("This PluginLoader does not support removal");
            }
        };
    }

    interface Combination extends StateAware, ModuleDescriptor {

    }
}
