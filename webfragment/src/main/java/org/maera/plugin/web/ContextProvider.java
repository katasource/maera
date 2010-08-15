package org.maera.plugin.web;

import org.maera.plugin.PluginParseException;

import java.util.Map;

/**
 * Main purpose of this is to add additional context entries for the web
 * fragment and make it available within the XML
 */
public interface ContextProvider {
    /**
     * Called after creation and autowiring.
     *
     * @param params The optional map of parameters specified in XML.
     */
    void init(Map<String, String> params) throws PluginParseException;

    /**
     * Gets the additional context map to make available for the web fragment
     *
     * @return context map for velocity templates
     */
    public Map<String, Object> getContextMap(Map<String, Object> context);
}
