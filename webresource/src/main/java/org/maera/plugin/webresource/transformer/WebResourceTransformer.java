package org.maera.plugin.webresource.transformer;

import org.dom4j.Element;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.servlet.DownloadableResource;

/**
 * Transforms a downloadable resource.  Instances are created per request, though it is assumed the resource will have
 * the appropriate caching headers to ensure the same user doesn't download it twice.
 *
 * @since 2.5.0
 */
public interface WebResourceTransformer {
    /**
     * Transforms the downloadable resource by returning a new one.
     *
     * @param configElement The element where it was used.  This is provided to allow the transformer to
     *                      take additional configuration in the form of custom attributes or sub-elements.
     * @param location      The original resource location
     * @param filePath      Extra path information.  Cannot be null, but can be an empty string if no extra path information
     * @param nextResource  The original resource
     * @return The new resource representing the transformed resource
     */
    DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource);
}
