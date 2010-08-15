package org.maera.plugin.web.descriptors;

import org.maera.plugin.web.WebFragmentHelper;
import org.maera.plugin.web.WebInterfaceManager;
import org.maera.plugin.web.model.WebPanel;

import java.util.List;
import java.util.Map;

public class MockWebInterfaceManager implements WebInterfaceManager {
    public boolean hasSectionsForLocation(String location) {
        return false;
    }

    public List getSections(String location) {
        return null;
    }

    public List getDisplayableSections(String location, Map context) {
        return null;
    }

    public List getItems(String section) {
        return null;
    }

    public List getDisplayableItems(String section, Map context) {
        return null;
    }

    public List<WebPanel> getDisplayableWebPanels(String location, Map<String, Object> context) {
        return null;
    }

    public List<WebPanel> getWebPanels(String location) {
        return null;
    }

    public void refresh() {
    }

    public WebFragmentHelper getWebFragmentHelper() {
        return new MockWebFragmentHelper();
    }
}
