package org.maera.plugin;

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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
public class TestStateAware extends TestCase {
    private Combination mockEnabling;
    private Combination mockDisabled;
    private ModuleDescriptor mockThwarted;
    private org.maera.plugin.manager.DefaultPluginManager manager;
    private Plugin plugin1;

    interface Combination extends StateAware, ModuleDescriptor {
    }

    ;

    protected void setUp() throws Exception {
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
        ArrayList pluginLoaders = new ArrayList();
        pluginLoaders.add(pluginLoader);

        ModuleDescriptorFactory moduleDescriptorFactory = mock(ModuleDescriptorFactory.class);

        manager = new org.maera.plugin.manager.DefaultPluginManager(new MemoryPluginPersistentStateStore(), pluginLoaders, moduleDescriptorFactory, new DefaultPluginEventManager());

    }

    /**
     * Any StateAware plugin module that is active when the plugin manager is initialised should
     * recieve an enabled message
     */
    public void testStateAwareOnInit() throws PluginParseException {
        plugin1.addModuleDescriptor(mockEnabling);
        plugin1.addModuleDescriptor(mockThwarted);
        plugin1.addModuleDescriptor(mockDisabled);
        manager.init();
        verify(mockEnabling).enabled();
    }

    /**
     * Any StateAware plugin moudle that is explicitly enabled or disabled through the plugin manager
     * should receive the appropriate message
     */
    public void testStateAwareOnPluginModule() throws PluginParseException {
        plugin1.addModuleDescriptor(mockDisabled);
        manager.init();

        when(mockDisabled.satisfiesMinJavaVersion()).thenReturn(true);
        manager.enablePluginModule(mockDisabled.getCompleteKey());
        verify(mockDisabled).enabled();

        manager.disablePluginModule(mockDisabled.getCompleteKey());
        verify(mockDisabled).disabled();
    }

    /**
     * If a plugin is disabled, any modules that are currently enabled should be sent the disabled
     * message
     */
    public void testStateAwareOnPluginDisable() throws PluginParseException {
        plugin1.addModuleDescriptor(mockEnabling);
        plugin1.addModuleDescriptor(mockDisabled);

        manager.init();
        verify(mockEnabling).enabled();

        manager.disablePlugin(plugin1.getKey());
        verify(mockEnabling).disabled();
    }

    /**
     * If a plugin is enabled, any modules that are currently enabled should be sent the enabled
     * message, but modules which are disabled should not.
     */
    public void testDisabledModuleDescriptorsAreEnabled() throws PluginParseException {
        plugin1.addModuleDescriptor(mockEnabling);
        plugin1.addModuleDescriptor(mockDisabled);
        plugin1.setEnabledByDefault(false);

        manager.init();

        manager.enablePlugin(plugin1.getKey());
        verify(mockEnabling).enabled();
    }

    private <T extends ModuleDescriptor> T makeMockModule(Class<T> moduleClass, String pluginKey, String moduleKey, boolean enabledByDefault) {
        ModuleDescriptor mock = Mockito.mock(moduleClass);
        when(mock.getKey()).thenReturn(moduleKey);
        when(mock.getCompleteKey()).thenReturn(pluginKey + ":" + moduleKey);
        when(mock.isEnabledByDefault()).thenReturn(enabledByDefault);
        return (T) mock;
    }

    private PluginLoader setupPluginLoader(final Plugin plugin1) {
        PluginLoader pluginLoader = new PluginLoader() { //TODO: should this deployer support removal and addition?

            public Collection loadAllPlugins(ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
                ArrayList list = new ArrayList();
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

            public Collection addFoundPlugins(ModuleDescriptorFactory moduleDescriptorFactory) {
                return null;
            }

            public void removePlugin(Plugin plugin) throws PluginException {
                throw new PluginException("This PluginLoader does not support removal");
            }
        };
        return pluginLoader;
    }
}
