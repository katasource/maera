package org.maera.plugin.descriptors;

public final class UnrecognisedModuleDescriptor extends AbstractModuleDescriptor<Void> {
    private String errorText;

    @Override
    public Void getModule() {
        return null;
    }

    @Override
    public boolean isEnabledByDefault() {
        //never enable a UnrecognisedModuleDescriptor
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
     * but it's required to construct an UnrecognisedModuleDescriptor when we don't have the XML Element.
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
     * but it's required to construct an UnrecognisedModuleDescriptor when we don't have the XML Element.
     *
     * @param name the name of the ModuleDescriptor
     */
    public void setName(final String name) {
        this.name = name;
    }
}
