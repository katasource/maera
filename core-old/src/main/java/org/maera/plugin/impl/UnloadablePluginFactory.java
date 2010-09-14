package org.maera.plugin.impl;

import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.descriptors.UnloadableModuleDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to create UnloadablePlugin instances.
 */
public final class UnloadablePluginFactory {
    /**
     * Creates an UnloadablePlugin instance from a given plugin, when there were problems loading the modules or the plugin itself
     *
     * @param oldPlugin the Plugin that is unloadable
     * @return UnloadablePlugin instance
     */
    public static UnloadablePlugin createUnloadablePlugin(final Plugin oldPlugin) {
        return createUnloadablePlugin(oldPlugin, null);
    }

    /**
     * Creates an UnloadablePlugin instance from a given plugin.
     * <p/>
     * It also allows a problematic ModuleDescriptor to be passed in, which will replace the existing
     * descriptor with the same key in the new plugin.
     *
     * @param oldPlugin            the Plugin that is unloadable
     * @param unloadableDescriptor the ModuleDescriptor containing the error
     * @return UnloadablePlugin instance
     */
    public static UnloadablePlugin createUnloadablePlugin(final Plugin oldPlugin, final UnloadableModuleDescriptor unloadableDescriptor) {
        final UnloadablePlugin newPlugin = new UnloadablePlugin();

        newPlugin.setName(oldPlugin.getName());
        newPlugin.setKey(oldPlugin.getKey());
        newPlugin.setI18nNameKey(oldPlugin.getI18nNameKey());
        newPlugin.setUninstallable(oldPlugin.isUninstallable());
        newPlugin.setDeletable(oldPlugin.isDeleteable());
        newPlugin.setPluginsVersion(oldPlugin.getPluginsVersion());

        // Make sure it's visible to the user
        newPlugin.setSystemPlugin(false);

        newPlugin.setPluginInformation(oldPlugin.getPluginInformation());

        final List<ModuleDescriptor<?>> moduleDescriptors = new ArrayList<ModuleDescriptor<?>>(oldPlugin.getModuleDescriptors());

        for (final ModuleDescriptor<?> descriptor : moduleDescriptors) {
            // If we find the module descriptor that is causing the problem, skip it
            if ((unloadableDescriptor != null) && descriptor.getKey().equals(unloadableDescriptor.getKey())) {
                continue;
            }
            newPlugin.addModuleDescriptor(descriptor);
        }

        // Add the unloadable descriptor to the end (if it exists)
        if (unloadableDescriptor != null) {
            newPlugin.addModuleDescriptor(unloadableDescriptor);
        }

        return newPlugin;
    }
}
