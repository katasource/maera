package org.maera.plugin;

/**
 * A plugin manager is responsible for retrieving plugins and modules, as well as managing plugin loading and state.
 *
 * @see PluginController
 * @see PluginAccessor
 * @see PluginSystemLifecycle
 * @deprecated since 2006-09-26 the preferred technique is to use the interfaces that this on e extends directly.
 */
@Deprecated
public interface PluginManager extends PluginController, PluginAccessor, PluginSystemLifecycle {
    /**
     * @deprecated since 2.2 - Please use {@link Descriptor#FILENAME} instead.
     */
    @Deprecated
    public static final String PLUGIN_DESCRIPTOR_FILENAME = PluginAccessor.Descriptor.FILENAME;
}
