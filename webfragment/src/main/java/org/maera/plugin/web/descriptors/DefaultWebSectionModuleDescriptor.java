package org.maera.plugin.web.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.web.WebInterfaceManager;

/**
 * Represents a web section - that is a collection of web items.
 */
public class DefaultWebSectionModuleDescriptor extends AbstractWebFragmentModuleDescriptor implements WebSectionModuleDescriptor {
    private String location;

    public DefaultWebSectionModuleDescriptor(final WebInterfaceManager webInterfaceManager) {
        super(webInterfaceManager);
    }

    public DefaultWebSectionModuleDescriptor() {

    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        super.init(plugin, element);

        location = element.attributeValue("location");
    }

    public String getLocation() {
        return location;
    }

    @Override
    public Class<Void> getModuleClass() {
        return Void.class;
    }

    @Override
    public Void getModule() {
        return null;
    }
}
