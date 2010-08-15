package org.maera.plugin.util;

import org.apache.commons.lang.Validate;
import org.dom4j.Element;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.descriptors.RequiresRestart;

import java.util.HashSet;
import java.util.Set;

/**
 * General plugin utility methods
 *
 * @since 2.1
 */
public class PluginUtils {
    public static final String ATLASSIAN_DEV_MODE = "atlassian.dev.mode";

    /**
     * System property for storing and retrieving the time the plugin system will wait for the enabling of a plugin in
     * seconds
     *
     * @since 2.3.6
     */
    public static final String ATLASSIAN_PLUGINS_ENABLE_WAIT = "atlassian.plugins.enable.wait";

    /**
     * Determines if a plugin requires a restart after being installed at runtime.  Looks for the annotation
     * {@link RequiresRestart} on the plugin's module descriptors.
     *
     * @param plugin The plugin that was just installed at runtime, but not yet enabled
     * @return True if a restart is required
     * @since 2.1
     */
    public static boolean doesPluginRequireRestart(final Plugin plugin) {
        //PLUG-451: When in dev mode, plugins should not require a restart.
        if (Boolean.getBoolean(ATLASSIAN_DEV_MODE)) {
            return false;
        }

        for (final ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors()) {
            if (descriptor.getClass().getAnnotation(RequiresRestart.class) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a list of all the module keys in a plugin that require restart.  Looks for the annotation
     * {@link RequiresRestart} on the plugin's module descriptors.
     *
     * @param plugin The plugin
     * @return A unique set of module keys
     * @since 2.5.0
     */
    public static Set<String> getPluginModulesThatRequireRestart(final Plugin plugin) {
        Set<String> keys = new HashSet<String>();
        for (final ModuleDescriptor<?> descriptor : plugin.getModuleDescriptors()) {
            if (descriptor.getClass().getAnnotation(RequiresRestart.class) != null) {
                keys.add(descriptor.getKey());
            }
        }
        return keys;
    }

    /**
     * Determines if a module element applies to the current application by matching the 'application' attribute
     * to the set of keys.  If the application is specified, but isn't in the set, we return false
     *
     * @param element The module element
     * @param keys    The set of application keys
     * @return True if it should apply, false otherwise
     * @since 2.2.0
     */
    public static boolean doesModuleElementApplyToApplication(Element element, Set<String> keys) {
        Validate.notNull(keys);
        Validate.notNull(element);
        String key = element.attributeValue("application");
        return !(key != null && !keys.contains(key));
    }

    /**
     * @return The default enabling waiting period in seconds
     * @since 2.3.6
     */
    public static int getDefaultEnablingWaitPeriod() {
        return Integer.parseInt(System.getProperty(ATLASSIAN_PLUGINS_ENABLE_WAIT, "60"));
    }
}
