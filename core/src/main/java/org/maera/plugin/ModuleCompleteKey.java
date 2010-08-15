/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Jul 29, 2004
 * Time: 3:19:33 PM
 */
package org.maera.plugin;

public class ModuleCompleteKey {
    private String pluginKey;
    private String moduleKey;

    public ModuleCompleteKey(String completeKey) {
        if (completeKey == null)
            throw new IllegalArgumentException("Invalid complete key specified: " + completeKey);

        final int sepIdx = completeKey.indexOf(":");

        if (sepIdx <= 0 || (sepIdx == completeKey.length() - 1))
            throw new IllegalArgumentException("Invalid complete key specified: " + completeKey);

        pluginKey = completeKey.substring(0, sepIdx);
        moduleKey = completeKey.substring(sepIdx + 1);
    }

    public String getModuleKey() {
        return moduleKey;
    }

    public String getPluginKey() {
        return pluginKey;
    }

    public String getCompleteKey() {
        return pluginKey + ":" + moduleKey;
    }
}