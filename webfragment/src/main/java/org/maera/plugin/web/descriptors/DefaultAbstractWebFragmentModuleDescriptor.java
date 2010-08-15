package org.maera.plugin.web.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.StateAware;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.web.Condition;
import org.maera.plugin.web.ContextProvider;
import org.maera.plugin.web.WebInterfaceManager;
import org.maera.plugin.web.model.WebLabel;
import org.maera.plugin.web.model.WebParam;

import java.util.List;
import java.util.Map;

/**
 * Wrapper for {@link WebFragmentModuleDescriptor}, so that it could be extended
 * by application specific wrappers to provide additional methods.
 */
public class DefaultAbstractWebFragmentModuleDescriptor implements StateAware, WebFragmentModuleDescriptor {
    private final WebFragmentModuleDescriptor decoratedDescriptor;

    public DefaultAbstractWebFragmentModuleDescriptor(final WebFragmentModuleDescriptor abstractDescriptor) {
        decoratedDescriptor = abstractDescriptor;
    }

    public void enabled() {
        decoratedDescriptor.enabled();
    }

    public void disabled() {
        decoratedDescriptor.disabled();
    }

    protected WebFragmentModuleDescriptor getDecoratedDescriptor() {
        return decoratedDescriptor;
    }

    public int getWeight() {
        return decoratedDescriptor.getWeight();
    }

    public String getKey() {
        return decoratedDescriptor.getKey();
    }

    public Void getModule() {
        return null;
    }

    public String getI18nNameKey() {
        return decoratedDescriptor.getI18nNameKey();
    }

    public String getDescriptionKey() {
        return decoratedDescriptor.getDescriptionKey();
    }

    public Plugin getPlugin() {
        return decoratedDescriptor.getPlugin();
    }

    public WebLabel getWebLabel() {
        return decoratedDescriptor.getWebLabel();
    }

    public WebLabel getTooltip() {
        return decoratedDescriptor.getTooltip();
    }

    public void setWebInterfaceManager(final WebInterfaceManager webInterfaceManager) {
        // bit of a hack but it works :)
        if (decoratedDescriptor instanceof AbstractWebFragmentModuleDescriptor) {
            final AbstractWebFragmentModuleDescriptor abstractWebFragmentModuleDescriptor = (AbstractWebFragmentModuleDescriptor) decoratedDescriptor;
            abstractWebFragmentModuleDescriptor.setWebInterfaceManager(webInterfaceManager);
        }
    }

    public Condition getCondition() {
        return decoratedDescriptor.getCondition();
    }

    public ContextProvider getContextProvider() {
        return decoratedDescriptor.getContextProvider();
    }

    public WebParam getWebParams() {
        return decoratedDescriptor.getWebParams();
    }

    // -----------------------------------------------------------------------------------------
    // ModuleDescriptor methods

    public String getCompleteKey() {
        return decoratedDescriptor.getCompleteKey();
    }

    public String getPluginKey() {
        return decoratedDescriptor.getPluginKey();
    }

    public String getName() {
        return decoratedDescriptor.getName();
    }

    public String getDescription() {
        return decoratedDescriptor.getDescription();
    }

    public Class<Void> getModuleClass() {
        return decoratedDescriptor.getModuleClass();
    }

    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        decoratedDescriptor.init(plugin, element);
    }

    public boolean isEnabledByDefault() {
        return decoratedDescriptor.isEnabledByDefault();
    }

    public boolean isSystemModule() {
        return decoratedDescriptor.isSystemModule();
    }

    public void destroy(final Plugin plugin) {
        decoratedDescriptor.destroy(plugin);
    }

    public Float getMinJavaVersion() {
        return decoratedDescriptor.getMinJavaVersion();
    }

    public boolean satisfiesMinJavaVersion() {
        return decoratedDescriptor.satisfiesMinJavaVersion();
    }

    public Map<String, String> getParams() {
        return decoratedDescriptor.getParams();
    }

    // ------------------------------------------------------------------------------------------------
    // Resourced methods

    public List<ResourceDescriptor> getResourceDescriptors() {
        return decoratedDescriptor.getResourceDescriptors();
    }

    /**
     * @deprecated since 2.5.0 use {@link #getResourceDescriptors()} and filter
     *             as required
     */
    @Deprecated
    public List<ResourceDescriptor> getResourceDescriptors(final String type) {
        return decoratedDescriptor.getResourceDescriptors(type);
    }

    public ResourceLocation getResourceLocation(final String type, final String name) {
        return decoratedDescriptor.getResourceLocation(type, name);
    }

    public ResourceDescriptor getResourceDescriptor(final String type, final String name) {
        return decoratedDescriptor.getResourceDescriptor(type, name);
    }

    @Override
    public String toString() {
        return decoratedDescriptor.toString();
    }
}
