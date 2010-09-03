package net.maera.plugin;

import net.maera.io.Resource;

import java.util.Collection;
import java.util.Locale;

/**
 * A Plugin is a software component that can be added to an application at any time - even at runtime.  It provides
 * additional behavior and components not present in the base host application, providing for an extensible platform
 * when building feature-rich and flexible applications.
 *
 * @since 0.1
 */
public interface Plugin {

    /**
     * Returns an application-unique identifier for the plugin instance.
     *
     * @return an application-unique identifier for the plugin instance.
     */
    String getId();

    /**
     * Returns the application key that uniquely identifies this type of plugin from all others.  The
     * {@code key} plus the {@link #getVersion() version} uniquely identifies a plugin instance from all others
     * (it is possible to have more than one version of the same plugin deployed).
     *
     * @return the application key that uniquely identifies this type of plugin from all others.
     */
    //TODO - rename this to something better? 'space'? 'region'? 'type'?... TBD
    String getKey();

    /**
     * Returns the locale-preferred display name of this plugin.  This would typically delegate to an i18n
     * mechanism to obtain the preferred name.  If there is no translation for the given locale, a
     * non-{@code null} default should be returned.
     *
     * @param locale the locale for which the translated name should be returned.
     * @return the name of the plugin based on the preferred locale.
     */
    String getName(Locale locale);

    /**
     * Returns the locale-preferred plugin description.  This would typically delegate to an i18n
     * mechanism to obtain the preferred description text.  If there is no translation for the given locale, a
     * non-{@code null} default should be returned.
     *
     * @param locale the locale for which the translated name should be returned.
     * @return the name of the plugin based on the preferred locale.
     */
    String getDescription(Locale locale);

    /**
     * Returns the plugin's version, ideally a version number compatible with
     * <a href="http://sling.apache.org/site/version-policy.html#VersionPolicy-VersionNumberSyntax">OSGi version
     * numbering</a>,
     * <a href="http://apr.apache.org/versioning.html">APR version numbering</a>, and/or
     * <a href="http://wiki.eclipse.org/index.php/Version_Numbering">Eclipse version numbering</a> conventions.
     * <p/>
     * The plugin's key and version together must be unique across all installed plugins.
     *
     * @return the plugin's version
     */
    String getVersion();

    //TODO - use these methods?  Or create a DeploymentConstraints interface representing what a plugin requires?
    //An interface returned would be more coarse-grained and resilient to additions over time, but may not be worth the trouble...
    String getHostMinVersion();
    String getHostMaxVersion();

    /**
     * Returns the current life cycle state of the plugin instance within the host application.
     *
     * @return the current life cycle state of the plugin instance within the host application.
     */
    PluginState getState();
       
    /**
     * Returns a {@code Resource} instance representing the specified plugin resource path.
     *
     * @param path the plugin-specific path where the resource is located
     * @return a {@code Resource} instance representing the specified plugin resource path.
     */
    Resource getResource(String path);

    /**
     * Will ask the container to create an instance of the given class, performing any necessary instance
     * configuration before returning.
     *
     * @param clazz the Class to instantiate. Cannot be null.
     * @return a newly instantiated instance of the given type.
     * @throws NullPointerException if the argument is null.
     */
    <T> T createInstance(Class<T> clazz) throws NullPointerException;

    /**
     * Gets all of the plugin's internally managed components of the specified type.
     *
     * @param clazz The type of object to retrieve, preferably an interface.
     * @param <T>   The desired instance type
     * @return A collection of implementations from the plugin's container
     */
    <T> Collection<T> getInstances(Class<T> clazz);
}
