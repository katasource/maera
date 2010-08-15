package org.maera.plugin.web;

import org.maera.plugin.Plugin;
import org.maera.plugin.web.conditions.ConditionLoadingException;

import java.util.List;
import java.util.Map;

/**
 * Provides application specific methods to build/render web fragments
 */
public interface WebFragmentHelper {
    /**
     * Creates a condition instance.  The following process should be used:<ol>
     * <li>Load the class via the plugin instance</li>
     * <li>Instantiate the class using the plugin if it implements {@link org.maera.plugin.AutowireCapablePlugin}
     * <li>If not, instantiate the class with the host container
     *
     * @param className the condition class name
     * @param plugin    the plugin from which the condition came
     * @return the condition instance
     * @throws ConditionLoadingException If the condition was unable to be created
     */
    Condition loadCondition(String className, Plugin plugin) throws ConditionLoadingException;

    /**
     * Creates a context provider instance.  The following process should be used:<ol>
     * <li>Load the class via the plugin instance</li>
     * <li>Instantiate the class using the plugin if it implements {@link org.maera.plugin.AutowireCapablePlugin}
     * <li>If not, instantiate the class with the host container
     *
     * @param className the context provider class name
     * @param plugin    the plugin from which the context provider came
     * @return the context provider instance
     * @throws ConditionLoadingException If the context provider was unable to be created
     */
    ContextProvider loadContextProvider(String className, Plugin plugin) throws ConditionLoadingException;

    /**
     * Look up a message key in the application
     *
     * @param key       The message key
     * @param arguments The arguments to use to replace tokens with any expressions already processed
     * @param context   The context (optional)
     * @return The text message
     */
    String getI18nValue(String key, List<?> arguments, Map<String, Object> context);

    /**
     * Renders the string fragment as a Velocity template
     *
     * @param fragment The string fragment to render
     * @param context  The context to use as the base of the Velocity context
     * @return The rendered string
     */
    String renderVelocityFragment(String fragment, Map<String, Object> context);
}
