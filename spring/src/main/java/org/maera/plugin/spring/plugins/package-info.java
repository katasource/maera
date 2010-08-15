/**
 * Contains classes that implement a new Spring namespace that allows beans to register themselves with the
 * {@link HostComponentProvider} by simply using the new "available" attribute in the Spring XML configuration.  When
 * using this namespace, there is no need to separately define a host component provider.
 *
 * <p>For example, this XML bean definition will make itself available to plugins to be auto-wired:
 * <pre>
 *   &lt;bean name="foo" class="my.Foo" plugin:available="true" /&gt;
 * </pre>
 */
package org.maera.plugin.spring.plugins;