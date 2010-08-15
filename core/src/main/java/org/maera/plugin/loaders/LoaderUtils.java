package org.maera.plugin.loaders;

import org.dom4j.Element;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.Resources;
import org.maera.plugin.elements.ResourceDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoaderUtils {
    /**
     * @deprecated use {@link org.maera.plugin.Resources#fromXml}
     */
    @Deprecated
    public static List<ResourceDescriptor> getResourceDescriptors(final Element element) throws PluginParseException {
        return Resources.fromXml(element).getResourceDescriptors();
    }

    public static Map<String, String> getParams(final Element element) {
        @SuppressWarnings("unchecked")
        final List<Element> elements = element.elements("param");

        final Map<String, String> params = new HashMap<String, String>(elements.size());

        for (final Element paramEl : elements) {
            final String name = paramEl.attributeValue("name");
            String value = paramEl.attributeValue("value");

            if ((value == null) && (paramEl.getTextTrim() != null) && !"".equals(paramEl.getTextTrim())) {
                value = paramEl.getTextTrim();
            }

            params.put(name, value);
        }

        return params;
    }
}
