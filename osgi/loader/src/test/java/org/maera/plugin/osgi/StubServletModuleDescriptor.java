package org.maera.plugin.osgi;

import org.maera.plugin.event.impl.DefaultPluginEventManager;
import org.maera.plugin.module.ModuleFactory;
import org.maera.plugin.servlet.DefaultServletModuleManager;
import org.maera.plugin.servlet.ServletModuleManager;
import org.maera.plugin.servlet.descriptors.ServletModuleDescriptor;

public class StubServletModuleDescriptor extends ServletModuleDescriptor {
    public StubServletModuleDescriptor() {
        this(ModuleFactory.LEGACY_MODULE_FACTORY, new DefaultServletModuleManager(new DefaultPluginEventManager()));
    }

    public StubServletModuleDescriptor(final ModuleFactory moduleCreator, final ServletModuleManager mgr) {
        super(moduleCreator, mgr);
    }
}
