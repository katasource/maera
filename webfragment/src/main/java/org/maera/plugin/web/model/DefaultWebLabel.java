package org.maera.plugin.web.model;

import org.dom4j.Element;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.web.ContextProvider;
import org.maera.plugin.web.WebFragmentHelper;
import org.maera.plugin.web.descriptors.WebFragmentModuleDescriptor;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * A simple bean to represent labels in the web interface.
 */
public class DefaultWebLabel extends DefaultWebParam implements WebLabel {
    private final String key;
    private final String noKeyValue;

    public DefaultWebLabel(Element labelEl, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, WebFragmentModuleDescriptor descriptor) throws PluginParseException {
        super(labelEl, webFragmentHelper, contextProvider, descriptor);
        if (labelEl == null) {
            throw new PluginParseException("You must specify a label for the section.");
        } else {
            this.key = labelEl.attributeValue("key");

            if (this.key == null) {
                this.noKeyValue = labelEl.getTextTrim();
            } else {
                this.noKeyValue = null;
            }
        }
    }

    public String getKey() {
        return key;
    }

    public String getNoKeyValue() {
        return noKeyValue;
    }

    public String getDisplayableLabel(HttpServletRequest req, Map<String, Object> origContext) {
        Map<String, Object> tmpContext = new HashMap<String, Object>(origContext);
        tmpContext.putAll(getContextMap(tmpContext));
        if (key != null) {
            if (params == null || params.isEmpty()) {
                return getWebFragmentHelper().getI18nValue(key, null, tmpContext);
            } else {
                List arguments = new ArrayList();

                // we know here because it's a tree map that the params are in alphabetical order
                for (Iterator iterator = params.keySet().iterator(); iterator.hasNext();) {
                    String key = (String) iterator.next();
                    if (key.startsWith("param"))
                        arguments.add(getWebFragmentHelper().renderVelocityFragment(params.get(key), tmpContext));
                }

                return getWebFragmentHelper().getI18nValue(key, arguments, tmpContext);
            }
        } else {
            return getWebFragmentHelper().renderVelocityFragment(noKeyValue, tmpContext);
        }
    }
}
