package org.maera.plugin.servlet.descriptors;

import com.mockobjects.dynamic.Mock;
import org.maera.plugin.Plugin;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.module.PrefixDelegatingModuleFactory;
import org.maera.plugin.module.PrefixModuleFactory;
import org.maera.plugin.servlet.ObjectFactories;
import org.maera.plugin.servlet.ObjectFactory;
import org.maera.plugin.servlet.PluginBuilder;
import org.maera.plugin.servlet.ServletModuleManager;

import javax.servlet.http.HttpServlet;
import java.util.*;

public class ServletModuleDescriptorBuilder {
    private Plugin plugin = new PluginBuilder().build();
    private String key = "test.servlet";
    private List<String> paths = new LinkedList<String>();
    private ServletModuleManager servletModuleManager = (ServletModuleManager) new Mock(ServletModuleManager.class).proxy();
    private Map<String, String> initParams = new HashMap<String, String>();
    private ObjectFactory<HttpServlet> servletFactory;

    public ServletModuleDescriptorBuilder with(Plugin plugin) {
        this.plugin = plugin;
        return this;
    }

    public ServletModuleDescriptorBuilder withKey(String key) {
        this.key = key;
        return this;
    }

    public ServletModuleDescriptorBuilder with(final HttpServlet servlet) {
        this.servletFactory = ObjectFactories.createSingleton(servlet);
        return this;
    }

    public ServletModuleDescriptorBuilder withFactory(ObjectFactory<HttpServlet> servlet) {
        this.servletFactory = servlet;
        return this;
    }

    public ServletModuleDescriptorBuilder withPath(String path) {
        paths.add(path);
        return this;
    }

    public ServletModuleDescriptorBuilder with(ServletModuleManager servletModuleManager) {
        this.servletModuleManager = servletModuleManager;
        return this;
    }

    public ServletModuleDescriptorBuilder withInitParam(String name, String value) {
        initParams.put(name, value);
        return this;
    }

    public ServletModuleDescriptor build() {
        Descriptor d = new Descriptor(plugin, key, servletFactory, immutableList(paths), immutableMap(initParams),
                servletModuleManager, new PrefixDelegatingModuleFactory(Collections.<PrefixModuleFactory>emptySet()));
        plugin.addModuleDescriptor(d);
        return d;
    }

    static <K, V> Map<K, V> immutableMap(Map<K, V> initParams) {
        return Collections.unmodifiableMap(new HashMap<K, V>(initParams));
    }

    <T> List<T> immutableList(List<T> list) {
        List<T> copy = new ArrayList<T>(list.size());
        copy.addAll(list);
        return Collections.unmodifiableList(copy);
    }

    static final class Descriptor extends ServletModuleDescriptor {
        final String key;
        final ObjectFactory<HttpServlet> servletFactory;
        final List<String> paths;
        final ServletModuleManager servletModuleManager;
        final Map<String, String> initParams;

        public Descriptor(
                Plugin plugin,
                String key,
                ObjectFactory<HttpServlet> servletFactory,
                List<String> paths,
                Map<String, String> initParams,
                ServletModuleManager servletModuleManager,
                ModuleFactory moduleFactory) {
            super(moduleFactory, servletModuleManager);
            this.plugin = plugin;
            this.key = key;
            this.servletFactory = servletFactory;
            this.paths = paths;
            this.initParams = initParams;
            this.servletModuleManager = servletModuleManager;
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
        public HttpServlet getModule() {
            return servletFactory.create();
        }


        @Override
        public List<String> getPaths() {
            return paths;
        }

        @Override
        public Map<String, String> getInitParams() {
            return initParams;
        }

    }

}
