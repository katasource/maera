package org.maera.plugin.servlet;

import org.maera.plugin.Plugin;
import org.maera.plugin.impl.StaticPlugin;

public class PluginBuilder {
    private String key = "test.plugin";

    public PluginBuilder withKey(String key) {
        this.key = key;
        return this;
    }

    public Plugin build() {
        StaticPlugin plugin = new StaticPlugin();
        plugin.setKey(key);
        return plugin;
    }
}
