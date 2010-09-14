package org.maera.plugin.descriptors;

import org.dom4j.Element;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;

/**
 * Instances of this class represent a module which <i>could not be loaded</i>, not a module
 * which <i>can be unloaded</i>.
 */
public class UnloadableModuleDescriptor extends AbstractModuleDescriptor<Void> {
    private String errorText;

    @Override
    public Void getModule() {
        return null;
    }

    @Override
    protected void loadClass(final Plugin plugin, final Element element) throws PluginParseException {
        // don't try to load the class -- we are possibly here because it doesn't exist
    }

    @Override
    public boolean isEnabledByDefault() {
        // An Unloadable module is never enabled
        return false;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(final String errorText) {
        this.errorText = errorText;
    }

    /**
     * Sets the key of the ModuleDescriptor
     * <p/>
     * This is theoretically bad, as the superclass and the interface doesn't define this method,
     * but it's required to construct an UnloadableModuleDescriptor when we don't have the XML Element.
     *
     * @param key the key of the ModuleDescriptor
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * Sets the name of the ModuleDescriptor
     * <p/>
     * This is theoretically bad, as the superclass and the interface doesn't define this method,
     * but it's required to construct an UnloadableModuleDescriptor when we don't have the XML Element.
     *
     * @param name the name of the ModuleDescriptor
     */
    public void setName(final String name) {
        this.name = name;
    }
}
