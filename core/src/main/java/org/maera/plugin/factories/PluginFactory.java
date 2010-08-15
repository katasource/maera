package org.maera.plugin.factories;

import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.loaders.classloading.DeploymentUnit;

/**
 * Creates the plugin artifact and deploys it into the appropriate plugin management system
 *
 * @since 2.0.0
 */
public interface PluginFactory {
    /**
     * Determines if this factory can handle this artifact.
     *
     * @param pluginArtifact The artifact to test
     * @return The plugin key, null if it cannot load the plugin
     * @throws org.maera.plugin.PluginParseException
     *          If there are exceptions parsing the plugin configuration when
     *          the deployer should have been able to deploy the plugin
     */
    String canCreate(PluginArtifact pluginArtifact) throws PluginParseException;

    /**
     * Deploys the deployment unit by instantiating the plugin and configuring it.  Should only be called if the respective
     * {@link #canCreate(PluginArtifact)} call returned the plugin key
     *
     * @param deploymentUnit          the unit to deploy
     * @param moduleDescriptorFactory the factory for the module descriptors
     * @return the plugin loaded from the deployment unit, or an UnloadablePlugin instance if loading fails.
     * @throws org.maera.plugin.PluginParseException
     *          if the plugin could not be parsed
     * @deprecated Since 2.2.0, use {@link #create(PluginArtifact,ModuleDescriptorFactory)} instead
     */
    Plugin create(DeploymentUnit deploymentUnit, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException;

    /**
     * Deploys the plugin artifact by instantiating the plugin and configuring it.  Should only be called if the respective
     * {@link #canCreate(PluginArtifact)} call returned the plugin key
     *
     * @param pluginArtifact          the plugin artifact to deploy
     * @param moduleDescriptorFactory the factory for the module descriptors
     * @return the plugin loaded from the plugin artifact, or an UnloadablePlugin instance if loading fails.
     * @throws org.maera.plugin.PluginParseException
     *          if the plugin could not be parsed
     * @since 2.2.0
     */
    Plugin create(PluginArtifact pluginArtifact, ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException;
}
