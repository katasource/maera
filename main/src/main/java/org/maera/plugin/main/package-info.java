/**
 * Main interface to the plugins framework providing a facade to hide the unnecessary internals.
 *
 * <p>
 * To use the facade, construct a {@link PluginsConfiguration} instance using the {@link PluginsConfigurationBuilder}
 * builder class.  Then, pass that instance into the constructor for the {@link MaeraPlugins} class, and call
 * {@link MaeraPlugins.start()}.  For example:
 * </p>
 * <pre>
 * PluginsConfiguration config = new PluginsConfigurationBuilder()
 *    .setPluginDirectory(new File("/my/plugin/directory"))
 *    .setPackagesToInclude("org.apache.*", "org.maera.*", "org.dom4j*")
 *    .build();
 * final MaeraPlugins plugins = new MaeraPlugins(config);
 * plugins.start();
 * </pre>
 * <p>
 * This code ensures only packages from Maera, Apache, and Dom4j are exposed to plugins. See the
 * {@link PluginsConfigurationBuilder} for more options.
 * </p>
 */
package org.maera.plugin.main;