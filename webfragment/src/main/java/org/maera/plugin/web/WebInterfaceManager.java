package org.maera.plugin.web;

import org.maera.plugin.web.descriptors.WebItemModuleDescriptor;
import org.maera.plugin.web.descriptors.WebSectionModuleDescriptor;
import org.maera.plugin.web.model.WebPanel;

import java.util.List;
import java.util.Map;

/**
 * A simple manager to provide sections of the web interface through plugins.
 */
public interface WebInterfaceManager {
    /**
     * @return True if there are any sections for the given location.
     */
    boolean hasSectionsForLocation(String location);

    /**
     * @return A list of all WebSectionModuleDescriptors for the given location.
     */
    List<WebSectionModuleDescriptor> getSections(String location);

    /**
     * @return A list of all AbstractWebLinkFragmentModuleDescriptor <i>viewable in a given context</i> in the given location.
     */
    List<WebSectionModuleDescriptor> getDisplayableSections(String location, Map<String, Object> context);

    /**
     * @return A list of all WebItemModuleDescriptors for the given section.
     */
    List<WebItemModuleDescriptor> getItems(String section);

    /**
     * @return A list of all AbstractWebLinkFragmentModuleDescriptor <i>viewable in a given context</i> in the given section.
     */
    List<WebItemModuleDescriptor> getDisplayableItems(String section, Map<String, Object> context);

    /**
     * @param location
     * @return A list of all {@link org.maera.plugin.web.model.WebPanel} module instances
     *         for the given location.
     */
    List<WebPanel> getWebPanels(String location);

    /**
     * @param location
     * @param context
     * @return A list of all {@link org.maera.plugin.web.model.WebPanel} module instances
     *         <i>viewable in a given context</i> in the given location.
     */
    List<WebPanel> getDisplayableWebPanels(String location, Map<String, Object> context);

    /**
     * Refresh the contents of the web interface manager.
     */
    void refresh();

    /**
     * @return The web fragment helper for this implementation.
     */
    WebFragmentHelper getWebFragmentHelper();
}
