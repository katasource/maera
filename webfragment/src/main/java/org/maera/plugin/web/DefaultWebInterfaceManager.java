package org.maera.plugin.web;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.PluginManager;
import org.maera.plugin.web.descriptors.*;
import org.maera.plugin.web.model.WebPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Stores and manages flexible web interface sections available in the system.
 */
public class DefaultWebInterfaceManager implements WebInterfaceManager {
    private PluginAccessor pluginAccessor;
    private WebFragmentHelper webFragmentHelper;
    private Map<String, List<WebSectionModuleDescriptor>> sections;
    private Map<String, List<WebItemModuleDescriptor>> items;
    private Map<String, List<DefaultWebPanelModuleDescriptor>> panels;
    private static final Logger log = LoggerFactory.getLogger(DefaultWebInterfaceManager.class);

    public static final WeightedDescriptorComparator WEIGHTED_DESCRIPTOR_COMPARATOR = new WeightedDescriptorComparator();

    public DefaultWebInterfaceManager() {
        refresh();
    }

    public DefaultWebInterfaceManager(PluginAccessor pluginAccessor, WebFragmentHelper webFragmentHelper) {
        this.pluginAccessor = pluginAccessor;
        this.webFragmentHelper = webFragmentHelper;
        refresh();
    }

    public boolean hasSectionsForLocation(String location) {
        return !getSections(location).isEmpty();
    }

    public List<WebSectionModuleDescriptor> getSections(String location) {
        if (location == null) {
            return Collections.emptyList();
        }

        List<WebSectionModuleDescriptor> result = sections.get(location);

        if (result == null) {
            result = new ArrayList<WebSectionModuleDescriptor>(); // use a tree map so we get nice weight sorting
            List<WebSectionModuleDescriptor> descriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(WebSectionModuleDescriptor.class);
            for (Iterator iterator = descriptors.iterator(); iterator.hasNext();) {
                WebSectionModuleDescriptor descriptor = (WebSectionModuleDescriptor) iterator.next();
                if (location.equalsIgnoreCase(descriptor.getLocation()))
                    result.add(descriptor);
            }

            Collections.sort(result, WEIGHTED_DESCRIPTOR_COMPARATOR);
            sections.put(location, result);
        }

        return result;
    }

    public List<WebSectionModuleDescriptor> getDisplayableSections(String location, Map<String, Object> context) {
        return filterFragmentsByCondition(getSections(location), context);
    }

    public List<WebItemModuleDescriptor> getItems(String section) {
        if (section == null) {
            return Collections.emptyList();
        }

        List<WebItemModuleDescriptor> result = items.get(section);

        if (result == null) {
            result = new ArrayList<WebItemModuleDescriptor>(); // use a tree map so we get nice weight sorting
            List<WebItemModuleDescriptor> descriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(WebItemModuleDescriptor.class);
            for (Iterator iterator = descriptors.iterator(); iterator.hasNext();) {
                WebItemModuleDescriptor descriptor = (WebItemModuleDescriptor) iterator.next();
                if (section.equalsIgnoreCase(descriptor.getSection()))
                    result.add(descriptor);
            }

            Collections.sort(result, WEIGHTED_DESCRIPTOR_COMPARATOR);
            items.put(section, result);
        }

        return result;
    }

    public List<WebItemModuleDescriptor> getDisplayableItems(String section, Map<String, Object> context) {
        return filterFragmentsByCondition(getItems(section), context);
    }

    public List<WebPanel> getDisplayableWebPanels(String location, Map<String, Object> context) {
        return toWebPanels(filterFragmentsByCondition(getWebPanelDescriptors(location), context));
    }

    public List<WebPanel> getWebPanels(String location) {
        return toWebPanels(getWebPanelDescriptors(location));
    }

    private List<WebPanel> toWebPanels(List<DefaultWebPanelModuleDescriptor> descriptors) {
        return Lists.transform(descriptors, new Function<DefaultWebPanelModuleDescriptor, WebPanel>() {
            public WebPanel apply(DefaultWebPanelModuleDescriptor from) {
                return from.getModule();
            }
        });
    }

    // TODO: probably succumb to Jed's gospel and refactor to Iterable

    private List<DefaultWebPanelModuleDescriptor> getWebPanelDescriptors(String location) {
        if (location == null) {
            return Collections.emptyList();
        } else {
            List<DefaultWebPanelModuleDescriptor> result = panels.get(location);
            if (result == null) {
                result = new ArrayList<DefaultWebPanelModuleDescriptor>(); // use a tree map so we get nice weight sorting

                List<DefaultWebPanelModuleDescriptor> descriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(DefaultWebPanelModuleDescriptor.class);
                for (DefaultWebPanelModuleDescriptor descriptor : descriptors) {
                    if (location.equalsIgnoreCase(descriptor.getLocation())) {
                        result.add(descriptor);
                    }
                }
                Collections.sort(result, WEIGHTED_DESCRIPTOR_COMPARATOR);
                panels.put(location, result);
            }
            return result;
        }
    }

    private <T extends ConditionalDescriptor> List<T> filterFragmentsByCondition(List<T> relevantItems, Map<String, Object> context) {
        if (relevantItems.isEmpty()) {
            return relevantItems;
        }

        List<T> result = new ArrayList<T>(relevantItems);
        for (Iterator<T> iterator = result.iterator(); iterator.hasNext();) {
            ConditionalDescriptor descriptor = iterator.next();
            try {
                if (descriptor.getCondition() != null && !descriptor.getCondition().shouldDisplay(context)) {
                    iterator.remove();
                }
            }
            catch (Throwable t) {
                log.error("Could not evaluate condition '" + descriptor.getCondition() + "' for descriptor: " + descriptor, t);
                iterator.remove();
            }
        }

        return result;
    }

    public void refresh() {
        sections = Collections.synchronizedMap(new HashMap());
        items = Collections.synchronizedMap(new HashMap());
        panels = Collections.synchronizedMap(new HashMap());
    }

    /**
     * @param pluginManager
     * @deprecated since 2.2.0, use {@link #setPluginAccessor(PluginAccessor)} instead
     */
    @Deprecated
    public void setPluginManager(PluginManager pluginManager) {
        setPluginAccessor(pluginManager);
    }

    /**
     * @param pluginAccessor The plugin accessor to set
     * @since 2.2.0
     */
    public void setPluginAccessor(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }

    public void setWebFragmentHelper(WebFragmentHelper webFragmentHelper) {
        this.webFragmentHelper = webFragmentHelper;
    }

    public WebFragmentHelper getWebFragmentHelper() {
        return webFragmentHelper;
    }

}
