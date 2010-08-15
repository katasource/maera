package org.maera.plugin.module;

import org.apache.commons.lang.Validate;
import org.maera.plugin.AutowireCapablePlugin;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.hostcontainer.HostContainer;

/**
 * Legacy module factory that uses the deprecated {@link AutowireCapablePlugin} interface
 *
 * @since 2.5.0
 */
public class HostContainerLegacyAdaptor extends LegacyModuleFactory {

    private final HostContainer hostContainer;

    public HostContainerLegacyAdaptor(HostContainer hostContainer) {
        Validate.notNull(hostContainer, "hostContainer should not be null");
        this.hostContainer = hostContainer;
    }

    public <T> T createModule(String name, ModuleDescriptor<T> moduleDescriptor) throws PluginParseException {

        // Give the plugin a go first
        if (moduleDescriptor.getPlugin() instanceof AutowireCapablePlugin) {
            return ((AutowireCapablePlugin) moduleDescriptor.getPlugin()).autowire(moduleDescriptor.getModuleClass());
        } else {
            return hostContainer.create(moduleDescriptor.getModuleClass());
        }
    }

}
