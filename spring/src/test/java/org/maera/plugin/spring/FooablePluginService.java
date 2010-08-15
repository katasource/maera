package org.maera.plugin.spring;

import org.maera.plugin.osgi.hostcomponents.ContextClassLoaderStrategy;

@AvailableToPlugins(contextClassLoaderStrategy = ContextClassLoaderStrategy.USE_PLUGIN)
public class FooablePluginService implements Fooable {
    public void sayHi() {
    }
}
