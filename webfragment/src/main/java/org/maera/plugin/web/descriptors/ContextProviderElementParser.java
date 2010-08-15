package org.maera.plugin.web.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.loaders.LoaderUtils;
import org.maera.plugin.util.Assertions;
import org.maera.plugin.web.ContextProvider;
import org.maera.plugin.web.WebFragmentHelper;
import org.maera.plugin.web.conditions.ConditionLoadingException;

/**
 * This class is used for constructing
 * {@link org.maera.plugin.web.ContextProvider} objects from a module
 * descriptor's XML element. Its functionality is used by both
 * {@link org.maera.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor}
 * and
 * {@link org.maera.plugin.web.descriptors.DefaultWebPanelModuleDescriptor}.
 *
 * @since 2.5.0
 */
class ContextProviderElementParser {
    private final WebFragmentHelper webFragmentHelper;

    public ContextProviderElementParser(final WebFragmentHelper webFragmentHelper) {
        this.webFragmentHelper = webFragmentHelper;
    }

    /**
     * @param element the module descriptor's XML element which contains the
     *                nested &lt;context-provider> element.
     * @return the configured {@link ContextProvider} instance, or
     *         <code>null</code> when the descriptor does not contain a
     *         &lt;context-provider> element.
     * @throws PluginParseException
     */
    public ContextProvider makeContextProvider(final Plugin plugin, final Element element) throws PluginParseException {
        Assertions.notNull("plugin == null", plugin);
        try {
            final Element contextProviderElement = element.element("context-provider");
            if (contextProviderElement == null) {
                return null;
            }
            final ContextProvider context = webFragmentHelper.loadContextProvider(contextProviderElement.attributeValue("class"), plugin);
            context.init(LoaderUtils.getParams(contextProviderElement));
            return context;
        }
        catch (final ClassCastException e) {
            throw new PluginParseException("Configured context-provider class does not implement the ContextProvider interface", e);
        }
        catch (final ConditionLoadingException cle) {
            throw new PluginParseException("Unable to load the module's display conditions: " + cle.getMessage(), cle);
        }
    }
}