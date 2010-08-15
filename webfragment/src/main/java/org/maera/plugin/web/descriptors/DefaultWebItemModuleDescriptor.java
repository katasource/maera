package org.maera.plugin.web.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.web.WebInterfaceManager;
import org.maera.plugin.web.model.DefaultWebIcon;
import org.maera.plugin.web.model.DefaultWebLink;
import org.maera.plugin.web.model.WebIcon;
import org.maera.plugin.web.model.WebLink;

/**
 * Represents a pluggable link.
 */
public class DefaultWebItemModuleDescriptor extends AbstractWebFragmentModuleDescriptor implements WebItemModuleDescriptor {
    private String section;
    private WebIcon icon;
    private DefaultWebLink link;
    private String styleClass;

    public DefaultWebItemModuleDescriptor(final WebInterfaceManager webInterfaceManager) {
        super(webInterfaceManager);
    }

    public DefaultWebItemModuleDescriptor() {
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        super.init(plugin, element);

        section = element.attributeValue("section");

        if (element.element("styleClass") != null) {
            styleClass = element.element("styleClass")
                    .getTextTrim();
        } else {
            styleClass = "";
        }
    }

    public String getSection() {
        return section;
    }

    public WebLink getLink() {
        return link;
    }

    public WebIcon getIcon() {
        return icon;
    }

    public String getStyleClass() {
        return styleClass;
    }

    @Override
    public void enabled() {
        super.enabled();

        // contextProvider is not available until the module is enabled because they may need to have dependencies injected
        if (element.element("icon") != null) {
            icon = new DefaultWebIcon(element.element("icon"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
        }
        if (element.element("link") != null) {
            link = new DefaultWebLink(element.element("link"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
        }
    }

    @Override
    public Void getModule() {
        return null;
    }
}
