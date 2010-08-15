package org.maera.plugin.servlet.descriptors;

import org.maera.plugin.Plugin;
import org.maera.plugin.hostcontainer.DefaultHostContainer;
import org.maera.plugin.module.ClassPrefixModuleFactory;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.module.PrefixDelegatingModuleFactory;
import org.maera.plugin.module.PrefixModuleFactory;
import org.maera.plugin.servlet.PluginBuilder;

import javax.servlet.ServletContextListener;
import java.util.Collections;

public class ServletContextListenerModuleDescriptorBuilder {
    private Plugin plugin = new PluginBuilder().build();
    private String key = "test.servlet.context.listener";
    private ServletContextListener listener;

    public ServletContextListenerModuleDescriptorBuilder with(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public ServletContextListenerModuleDescriptorBuilder withKey(String key) {
        this.key = key;
        return this;
    }

    public ServletContextListenerModuleDescriptorBuilder with(ServletContextListener listener) {
        this.listener = listener;
        return this;
    }

    public ServletContextListenerModuleDescriptor build() {
        PrefixDelegatingModuleFactory prefixDelegatingModuleFactory = new PrefixDelegatingModuleFactory(
                Collections.<PrefixModuleFactory>singleton(new ClassPrefixModuleFactory(new DefaultHostContainer())));
        Descriptor d = new Descriptor(plugin, key, listener, prefixDelegatingModuleFactory);
        plugin.addModuleDescriptor(d);
        return d;
    }

    private static final class Descriptor extends ServletContextListenerModuleDescriptor {
        final Plugin plugin;
        final String key;
        final ServletContextListener listener;

        public Descriptor(
                Plugin plugin,
                String key,
                ServletContextListener listener, ModuleFactory moduleFactory) {
            super(moduleFactory);
            this.plugin = plugin;
            this.key = key;
            this.listener = listener;
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }

        @Override
        public String getCompleteKey() {
            return getPluginKey() + ":" + key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public ServletContextListener getModule() {
            return listener;
        }
    }
}
