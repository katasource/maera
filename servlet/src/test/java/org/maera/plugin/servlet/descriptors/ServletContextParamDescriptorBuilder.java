package org.maera.plugin.servlet.descriptors;

import org.maera.plugin.Plugin;
import org.maera.plugin.servlet.PluginBuilder;

public class ServletContextParamDescriptorBuilder {
    private Plugin plugin = new PluginBuilder().build();
    private String key = "test.servlet.context.param";
    private String paramName;
    private String paramValue;

    public ServletContextParamDescriptorBuilder with(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public ServletContextParamDescriptorBuilder withKey(String key) {
        this.key = key;
        return this;
    }

    public ServletContextParamDescriptorBuilder withParam(String name, String value) {
        paramName = name;
        paramValue = value;
        return this;
    }

    public ServletContextParamModuleDescriptor build() {
        ModuleDescriptor d = new ModuleDescriptor(plugin, key, paramName, paramValue);
        plugin.addModuleDescriptor(d);
        return d;
    }

    private static final class ModuleDescriptor extends ServletContextParamModuleDescriptor {
        final String key;
        final String name;
        final String value;

        public ModuleDescriptor(
                Plugin plugin,
                String key,
                String name,
                String value) {
            this.plugin = plugin;
            this.key = key;
            this.name = name;
            this.value = value;
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
        public String getParamName() {
            return name;
        }

        @Override
        public String getParamValue() {
            return value;
        }
    }
}
