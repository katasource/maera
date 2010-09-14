package org.maera.plugin.mock;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;

public class MockMineralModuleDescriptor extends AbstractModuleDescriptor<MockMineral> {
    String weight;

    public MockMineralModuleDescriptor() {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        super.init(plugin, element);
        if (element.element("weight") != null) {
            weight = element.element("weight").getTextTrim();
        }
    }

    @Override
    public MockMineral getModule() {
        return new MockGold(Integer.parseInt(weight));
    }
}