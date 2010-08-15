package org.maera.plugin.web.descriptors;

import org.dom4j.Element;

/**
 * This class contains the logic for reading the weight value from a module
 * descriptor's XML element.
 * Its functionality is used by both
 * {@link org.maera.plugin.web.descriptors.AbstractWebFragmentModuleDescriptor}
 * and {@link org.maera.plugin.web.descriptors.DefaultWebPanelModuleDescriptor}.
 *
 * @since 2.5.0
 */
class WeightElementParser {
    public static final int DEFAULT_WEIGHT = 1000;

    /**
     * @param moduleDescriptorElement a module descriptor XML element.
     * @return the value of the <code>weight</code> attribute of the
     *         specified module descriptor element, or the system's default weight
     *         value if no weight was specified.
     */
    public static int getWeight(Element moduleDescriptorElement) {
        try {
            return Integer.parseInt(moduleDescriptorElement.attributeValue("weight"));
        }
        catch (final NumberFormatException e) {
            return DEFAULT_WEIGHT;
        }
    }
}