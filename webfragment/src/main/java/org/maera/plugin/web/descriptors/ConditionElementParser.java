package org.maera.plugin.web.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.loaders.LoaderUtils;
import org.maera.plugin.util.Assertions;
import org.maera.plugin.web.Condition;
import org.maera.plugin.web.WebFragmentHelper;
import org.maera.plugin.web.conditions.*;

import java.util.Iterator;
import java.util.List;

/**
 * This class contains the logic for constructing
 * {@link org.maera.plugin.web.Condition} objects from a module descriptor's
 * XML element. Its functionality is used by both
 * {@link org.maera.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor}
 * and
 * {@link org.maera.plugin.web.descriptors.DefaultWebPanelModuleDescriptor}.
 *
 * @since 2.5.0
 */
class ConditionElementParser {
    static class CompositeType {
        static final int OR = 0;
        static final int AND = 1;

        static int parse(final String type) throws PluginParseException {
            if ("or".equalsIgnoreCase(type)) {
                return CompositeType.OR;
            } else if ("and".equalsIgnoreCase(type)) {
                return CompositeType.AND;
            } else {
                throw new PluginParseException("Invalid condition type specified. type = " + type);
            }
        }

    }

    private final WebFragmentHelper webFragmentHelper;

    public ConditionElementParser(final WebFragmentHelper webFragmentHelper) {
        this.webFragmentHelper = webFragmentHelper;
    }

    /**
     * Create a condition for when this web fragment should be displayed.
     *
     * @param element Element of web-section, web-item, or web-panel.
     * @param type    logical operator type {@link #getCompositeType}
     * @throws org.maera.plugin.PluginParseException
     *
     */
    @SuppressWarnings("unchecked")
    public Condition makeConditions(final Plugin plugin, final Element element, final int type) throws PluginParseException {
        Assertions.notNull("plugin == null", plugin);

        // make single conditions (all Anded together)
        final List<Element> singleConditionElements = element.elements("condition");
        Condition singleConditions = null;
        if ((singleConditionElements != null) && !singleConditionElements.isEmpty()) {
            singleConditions = makeConditions(plugin, singleConditionElements, type);
        }

        // make composite conditions (logical operator can be specified by
        // "type")
        final List<Element> nestedConditionsElements = element.elements("conditions");
        AbstractCompositeCondition nestedConditions = null;
        if ((nestedConditionsElements != null) && !nestedConditionsElements.isEmpty()) {
            nestedConditions = getCompositeCondition(type);
            for (final Iterator<Element> iterator = nestedConditionsElements.iterator(); iterator.hasNext();) {
                final Element nestedElement = iterator.next();
                nestedConditions.addCondition(makeConditions(plugin, nestedElement, CompositeType.parse(nestedElement.attributeValue("type"))));
            }
        }

        if ((singleConditions != null) && (nestedConditions != null)) {
            // Join together the single and composite conditions by this type
            final AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
            compositeCondition.addCondition(singleConditions);
            compositeCondition.addCondition(nestedConditions);
            return compositeCondition;
        } else if (singleConditions != null) {
            return singleConditions;
        } else if (nestedConditions != null) {
            return nestedConditions;
        }

        return null;
    }

    public Condition makeConditions(final Plugin plugin, final List<Element> elements, final int type) throws PluginParseException {
        if (elements.isEmpty()) {
            return null;
        } else if (elements.size() == 1) {
            return makeCondition(plugin, elements.get(0));
        } else {
            final AbstractCompositeCondition compositeCondition = getCompositeCondition(type);
            for (final Iterator<Element> it = elements.iterator(); it.hasNext();) {
                final Element element = it.next();
                compositeCondition.addCondition(makeCondition(plugin, element));
            }

            return compositeCondition;
        }
    }

    public Condition makeCondition(final Plugin plugin, final Element element) throws PluginParseException {
        try {
            final Condition condition = webFragmentHelper.loadCondition(element.attributeValue("class"), plugin);
            condition.init(LoaderUtils.getParams(element));

            if ((element.attribute("invert") != null) && "true".equals(element.attributeValue("invert"))) {
                return new InvertedCondition(condition);
            }

            return condition;
        }
        catch (final ClassCastException e) {
            throw new PluginParseException("Configured condition class does not implement the Condition interface", e);
        }
        catch (final ConditionLoadingException cle) {
            throw new PluginParseException("Unable to load the module's display conditions: " + cle.getMessage(), cle);
        }
    }

    private AbstractCompositeCondition getCompositeCondition(final int type) throws PluginParseException {
        switch (type) {
            case CompositeType.OR:
                return new OrCompositeCondition();
            case CompositeType.AND:
                return new AndCompositeCondition();
            default:
                throw new PluginParseException("Invalid condition type specified. type = " + type);
        }
    }
}