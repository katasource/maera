package org.maera.plugin.servlet;

/**
 * Identifies a resource in the plugin system. Normally generated from a request URL.
 *
 * @deprecated since 2.2
 */
class PluginResource {
    private final String moduleCompleteKey;
    private final String resourceName;

    /**
     * @param moduleCompleteKey the key of the plugin module where the resource can be found, or the key
     *                          of the plugin if the resource is specified at the plugin level.
     * @param resourceName      the name of the resource.
     */
    public PluginResource(String moduleCompleteKey, String resourceName) {
        this.moduleCompleteKey = moduleCompleteKey;
        this.resourceName = resourceName;
    }

    public String getModuleCompleteKey() {
        return moduleCompleteKey;
    }

    public String getResourceName() {
        return resourceName;
    }


    public String toString() {
        return "[moduleCompleteKey=" + moduleCompleteKey + ", resourceName=" + resourceName + "]";
    }
}
