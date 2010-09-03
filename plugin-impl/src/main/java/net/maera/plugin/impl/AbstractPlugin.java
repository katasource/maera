package net.maera.plugin.impl;

import net.maera.plugin.Plugin;
import net.maera.plugin.PluginState;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @since 0.1
 * @author Les Hazlewood
 */
public abstract class AbstractPlugin implements Plugin {

    private final AtomicReference<PluginState> stateRef;

    public AbstractPlugin() {
        stateRef = new AtomicReference<PluginState>(PluginState.UNINSTALLED);
    }

    @Override
    public PluginState getState() {
        return stateRef.get();
    }

    protected void setState(PluginState state) {
        stateRef.set(state);
    }



}
