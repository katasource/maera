package org.maera.plugin.mock;

import junit.framework.Assert;
import org.dom4j.Element;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.StateAware;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;

public class MockAnimalModuleDescriptor extends AbstractModuleDescriptor<MockAnimal> implements StateAware, ModuleDescriptor<MockAnimal> {
    MockAnimal module;
    public boolean disabled;
    public boolean enabled;

    private final String type;
    private final String name;


    public MockAnimalModuleDescriptor() {
        this(null, null);
    }

    public MockAnimalModuleDescriptor(String type, String name) {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
        this.type = type;
        this.name = name;
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        super.init(plugin, element);
        if (type != null && name != null) {
            Assert.assertNotNull(plugin.getResourceDescriptor(type, name));
        }
    }

    @Override
    public MockAnimal getModule() {
        if (module == null) {
            try {
                module = getModuleClass().newInstance();
            }
            catch (final InstantiationException e) {
                throw new PluginParseException(e);
            }
            catch (final IllegalAccessException e) {
                throw new PluginParseException(e);
            }
        }
        return module;
    }

    @Override
    public void enabled() {
        super.enabled();
        enabled = true;
    }

    @Override
    public void disabled() {
        disabled = true;
        super.disabled();
    }

    public boolean isEnabled() {
        return enabled && !disabled;
    }
}
