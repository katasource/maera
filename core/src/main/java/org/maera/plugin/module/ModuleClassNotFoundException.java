package org.maera.plugin.module;

import org.maera.plugin.PluginParseException;

/**
 * If a module class could not be found
 *
 * @since 2.5
 */
public class ModuleClassNotFoundException extends PluginParseException {
    private final String className;
    private final String pluginKey;
    private final String moduleKey;
    private String errorMsg;

    public ModuleClassNotFoundException(String className, String pluginKey, String moduleKey, Exception ex, String errorMsg) {
        super(ex);
        this.className = className;
        this.pluginKey = pluginKey;
        this.moduleKey = moduleKey;
        this.errorMsg = errorMsg;
    }

    public String getClassName() {
        return className;
    }

    public String getPluginKey() {
        return pluginKey;
    }

    public String getModuleKey() {
        return moduleKey;
    }

    @Override
    public String getMessage() {
        return errorMsg;
    }
}
