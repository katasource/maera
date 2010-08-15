package org.maera.plugin.web.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.StateAware;
import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.web.Condition;
import org.maera.plugin.web.ContextProvider;
import org.maera.plugin.web.WebInterfaceManager;
import org.maera.plugin.web.model.DefaultWebLabel;
import org.maera.plugin.web.model.DefaultWebParam;
import org.maera.plugin.web.model.WebLabel;
import org.maera.plugin.web.model.WebParam;

import java.util.List;

/**
 * An abstract convenience class for web fragment descriptors.
 */
public abstract class AbstractWebFragmentModuleDescriptor extends AbstractModuleDescriptor<Void> implements StateAware, WebFragmentModuleDescriptor {
    protected WebInterfaceManager webInterfaceManager;
    protected Element element;
    protected int weight;

    protected Condition condition;
    protected ContextProvider contextProvider;
    protected DefaultWebLabel label;
    protected DefaultWebLabel tooltip;
    protected WebParam params;
    private ConditionElementParser conditionElementParser;
    private ContextProviderElementParser contextProviderElementParser;

    protected AbstractWebFragmentModuleDescriptor(final WebInterfaceManager webInterfaceManager) {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
        setWebInterfaceManager(webInterfaceManager);
    }

    public AbstractWebFragmentModuleDescriptor() {
        super(ModuleFactory.LEGACY_MODULE_FACTORY);
    }

    @Override
    public void init(final Plugin plugin, final Element element) throws PluginParseException {
        super.init(plugin, element);

        this.element = element;
        weight = WeightElementParser.getWeight(element);
    }

    /**
     * Create a condition for when this web fragment should be displayed
     *
     * @param element Element of web-section or web-item
     * @param type    logical operator type
     *                {@link ConditionElementParser#get(String)}
     * @throws PluginParseException
     */
    protected Condition makeConditions(final Element element, final int type) throws PluginParseException {
        return getRequiredConditionElementParser().makeConditions(plugin, element, type);
    }

    @SuppressWarnings("unchecked")
    protected Condition makeConditions(final List elements, final int type) throws PluginParseException {
        return getRequiredConditionElementParser().makeConditions(plugin, elements, type);
    }

    protected Condition makeCondition(final Element element) throws PluginParseException {
        return getRequiredConditionElementParser().makeCondition(plugin, element);
    }

    protected ContextProvider makeContextProvider(final Element element) throws PluginParseException {
        return contextProviderElementParser.makeContextProvider(plugin, element.getParent());
    }

    private ConditionElementParser getRequiredConditionElementParser() {
        if (conditionElementParser == null) {
            throw new IllegalStateException("ModuleDescriptorHelper not available because the WebInterfaceManager has not been injected.");
        } else {
            return conditionElementParser;
        }
    }

    @Override
    public void enabled() {
        super.enabled();
        // this was moved to the enabled() method because spring beans declared
        // by the plugin are not available for injection during the init() phase
        try {
            contextProvider = contextProviderElementParser.makeContextProvider(plugin, element);

            if (element.element("label") != null) {
                label = new DefaultWebLabel(element.element("label"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
            }

            if (element.element("tooltip") != null) {
                tooltip = new DefaultWebLabel(element.element("tooltip"), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
            }

            if (getParams() != null) {
                params = new DefaultWebParam(getParams(), webInterfaceManager.getWebFragmentHelper(), contextProvider, this);
            }

            condition = makeConditions(element, ConditionElementParser.CompositeType.AND);
        }
        catch (final PluginParseException e) {
            // is there a better exception to throw?
            throw new RuntimeException("Unable to enable web fragment", e);
        }

        webInterfaceManager.refresh();
    }

    @Override
    public void disabled() {
        webInterfaceManager.refresh();
        super.disabled();
    }

    public int getWeight() {
        return weight;
    }

    public WebLabel getWebLabel() {
        return label;
    }

    public WebLabel getTooltip() {
        return tooltip;
    }

    public void setWebInterfaceManager(final WebInterfaceManager webInterfaceManager) {
        this.webInterfaceManager = webInterfaceManager;
        this.conditionElementParser = new ConditionElementParser(webInterfaceManager.getWebFragmentHelper());
        this.contextProviderElementParser = new ContextProviderElementParser(webInterfaceManager.getWebFragmentHelper());
    }

    public Condition getCondition() {
        return condition;
    }

    public ContextProvider getContextProvider() {
        return contextProvider;
    }

    public WebParam getWebParams() {
        return params;
    }
}
