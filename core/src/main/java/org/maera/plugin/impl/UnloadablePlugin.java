package org.maera.plugin.impl;

/**
 * This class represents a Plugin that was not able to be loaded by the PluginManager.
 *
 * @see org.maera.plugin.manager.DefaultPluginManager
 */
public class UnloadablePlugin extends StaticPlugin {
    private static final String UNKNOWN_KEY_PREFIX = "Unknown-";
    private String errorText;
    private boolean uninstallable = true;
    private boolean deletable = true;

    public UnloadablePlugin() {
        this(null);
    }

    /**
     * @param text The error text
     * @since 2.0.0
     */
    public UnloadablePlugin(final String text) {
        errorText = text;
        setKey(UNKNOWN_KEY_PREFIX + System.identityHashCode(this));
    }

    @Override
    public boolean isUninstallable() {
        return uninstallable;
    }

    public void setDeletable(final boolean deletable) {
        this.deletable = deletable;
    }

    @Override
    public boolean isDeleteable() {
        return deletable;
    }

    public void setUninstallable(final boolean uninstallable) {
        this.uninstallable = uninstallable;
    }

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    public String getErrorText() {
        return errorText;
    }

    public void setErrorText(final String errorText) {
        this.errorText = errorText;
    }

    @Override
    public void close() {
    }

    @Override
    protected void uninstallInternal() {
        if (!uninstallable) {
            // should throw exception
            super.uninstallInternal();
        }
    }

    @Override
    public String toString() {
        return super.toString() + " " + errorText;
    }
}
