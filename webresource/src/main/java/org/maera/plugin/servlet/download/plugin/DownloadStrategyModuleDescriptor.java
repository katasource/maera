package org.maera.plugin.servlet.download.plugin;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.servlet.DownloadStrategy;

/**
 * A plugin module which provides a {@link DownloadStrategy}.
 *
 * @see DownloadStrategy
 * @see PluggableDownloadStrategy
 * @since 2.2.0
 */
public class DownloadStrategyModuleDescriptor extends AbstractModuleDescriptor<DownloadStrategy> {
    /**
     * Creates a download strategy.
     *
     * @param moduleCreator The factory to create module instances
     * @Since 2.5.0
     */
    public DownloadStrategyModuleDescriptor(ModuleFactory moduleCreator) {
        super(moduleCreator);
    }

    public DownloadStrategy getModule() {
        return moduleFactory.createModule(moduleClassName, this);
    }
}
