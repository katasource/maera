package org.maera.plugin.osgi;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.module.ModuleFactory;

import java.util.concurrent.Callable;

public class CallableModuleDescriptor extends AbstractModuleDescriptor<Callable> {
    public CallableModuleDescriptor(ModuleFactory moduleCreator) {
        super(moduleCreator);
    }

    @Override
    public Callable getModule() {
        return moduleFactory.createModule(moduleClassName, this);
    }
}
