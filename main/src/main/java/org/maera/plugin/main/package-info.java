/**
 * Main interface to the plugins framework providing a facade to hide the unnecessary internals.
 *
 * <p>
 * To use the facade, construct a {@link PluginsConfiguration} instance using the {@link PluginsConfigurationBuilder}
 * builder class.  Then, pass that instance into the constructor for the {@link AtlassianPlugins} class, and call
 * {@link AtlassianPlugins.start()}.  For example:
 * </p>
 * <pre>
 * PluginsConfiguration config = new PluginsConfigurationBuilder()
 *    .setPluginDirectory(new File("/my/plugin/directory"))
 *    .setPackagesToInclude("org.apache.*", "com.atlassian.*", "org.dom4j*")
 *    .build();
 * final AtlassianPlugins plugins = new AtlassianPlugins(config);
 * plugins.start();
 * </pre>
 * <p>
 * This code ensures only packages from Atlassian, Apache, and Dom4j are exposed to plugins. See the
 * {@link PluginsConfigurationBuilder} for more options.
 * </p>
 *
 * @since 2.2.0
 */
package org.maera.plugin.main;