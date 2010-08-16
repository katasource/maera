package org.maera.plugin.factories;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginParseException;

import java.io.File;

public class LegacyDynamicPluginFactoryTest {

    @Test(expected = PluginParseException.class)
    public void testCreateCorruptJar() {
        final LegacyDynamicPluginFactory factory = new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME);
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);

        Mock mockArtifact = new Mock(PluginArtifact.class);
        mockArtifact.expectAndReturn("getResourceAsStream", C.ANY_ARGS, null);
        mockArtifact.expectAndReturn("toFile", new File("sadfasdf"));
        factory.create((PluginArtifact) mockArtifact.proxy(), (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy());
    }
}
