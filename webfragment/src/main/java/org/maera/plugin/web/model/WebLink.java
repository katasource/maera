package org.maera.plugin.web.model;

import org.maera.plugin.web.descriptors.WebFragmentModuleDescriptor;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface WebLink {
    String getRenderedUrl(Map<String, Object> context);

    String getDisplayableUrl(HttpServletRequest req, Map<String, Object> context);

    boolean hasAccessKey();

    String getAccessKey(Map<String, Object> context);

    String getId();

    WebFragmentModuleDescriptor getDescriptor();
}
