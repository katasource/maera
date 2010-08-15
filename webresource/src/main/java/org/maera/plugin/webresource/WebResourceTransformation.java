package org.maera.plugin.webresource;

import org.apache.commons.lang.Validate;
import org.dom4j.Element;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.servlet.DownloadableResource;
import org.maera.plugin.webresource.transformer.WebResourceTransformerModuleDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a set of transformer invocations for a specific web resource set and extension.  Transformers are retrieved
 * from the plugin system on request, not plugin initialisation, since plugin start order is indeterminate.
 *
 * @since 2.5.0
 */
class WebResourceTransformation {
    private final String extension;
    private final Map<String, Element> transformerElements;
    private Logger log = LoggerFactory.getLogger(WebResourceTransformation.class);

    WebResourceTransformation(Element element) {
        Validate.notNull(element.attribute("extension"));

        this.extension = "." + element.attributeValue("extension");

        LinkedHashMap<String, Element> transformers = new LinkedHashMap<String, Element>();
        for (Element transformElement : (List<Element>) element.elements("transformer")) {
            transformers.put(transformElement.attributeValue("key"), transformElement);
        }
        transformerElements = Collections.unmodifiableMap(transformers);
    }

    boolean matches(ResourceLocation location) {
        String loc = location.getLocation();
        if (loc == null || "".equals(loc.trim())) {
            loc = location.getName();
        }
        return loc.endsWith(extension);
    }

    DownloadableResource transformDownloadableResource(PluginAccessor pluginAccessor, DownloadableResource resource, ResourceLocation resourceLocation, String filePath) {
        DownloadableResource lastResource = resource;
        for (Map.Entry<String, Element> entry : transformerElements.entrySet()) {
            boolean found = false;
            for (WebResourceTransformerModuleDescriptor descriptor : pluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceTransformerModuleDescriptor.class)) {
                if (descriptor.getKey().equals(entry.getKey())) {
                    found = true;
                    lastResource = descriptor.getModule().transform(entry.getValue(), resourceLocation, filePath, lastResource);
                }
            }
            if (!found) {
                log.warn("Web resource transformer " + entry.getKey() + " not found for resource " + resourceLocation.getName() + ", skipping");
            }
        }
        return lastResource;
    }

}
