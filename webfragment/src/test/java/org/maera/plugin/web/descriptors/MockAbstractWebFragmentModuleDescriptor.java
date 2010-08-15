package org.maera.plugin.web.descriptors;

import org.maera.plugin.web.WebInterfaceManager;

public class MockAbstractWebFragmentModuleDescriptor extends AbstractWebFragmentModuleDescriptor {
    protected MockAbstractWebFragmentModuleDescriptor(final WebInterfaceManager webInterfaceManager) {
        super(webInterfaceManager);
    }

    @Override
    public Void getModule() {
        return null;
    }
}
