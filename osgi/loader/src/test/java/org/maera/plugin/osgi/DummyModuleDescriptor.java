package org.maera.plugin.osgi;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;

public class DummyModuleDescriptor extends AbstractModuleDescriptor<Void> {
    private boolean enabled = false;

    @Override
    public Void getModule() {
        return null;
    }

    @Override
    public void enabled() {
        super.enabled();
        enabled = true;

    }

    public boolean isEnabled() {
        return enabled;
    }

}
