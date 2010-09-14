package org.maera.plugin.mock;

import org.maera.plugin.descriptors.AbstractModuleDescriptor;
import org.maera.plugin.descriptors.CannotDisable;

@CannotDisable
public class MockVegetableModuleDescriptor extends AbstractModuleDescriptor<MockThing> {
    @Override
    public MockThing getModule() {
        return new MockVegetable();
    }
}
