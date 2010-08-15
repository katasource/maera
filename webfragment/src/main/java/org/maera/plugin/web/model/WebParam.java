package org.maera.plugin.web.model;

import org.maera.plugin.web.descriptors.WebFragmentModuleDescriptor;

import java.util.Map;
import java.util.SortedMap;

/**
 * Represents arbitrary number of key/value pairs
 */
public interface WebParam {
    SortedMap<String, String> getParams();

    Object get(String key);

    String getRenderedParam(String paramKey, Map<String, Object> context);

    WebFragmentModuleDescriptor getDescriptor();
}
