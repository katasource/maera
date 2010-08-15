package org.maera.plugin.factories;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginParseException;

import java.io.File;

public class TestLegacyDynamicPluginFactory extends TestCase {
    public void testCreateCorruptJar() {
        final LegacyDynamicPluginFactory factory = new LegacyDynamicPluginFactory(PluginAccessor.Descriptor.FILENAME);
        final Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        try {
            Mock mockArtifact = new Mock(PluginArtifact.class);
            mockArtifact.expectAndReturn("getResourceAsStream", C.ANY_ARGS, null);
            mockArtifact.expectAndReturn("toFile", new File("sadfasdf"));
            factory.create((PluginArtifact) mockArtifact.proxy(), (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy());
            fail("Should have thrown an exception");
        }
        catch (final PluginParseException ex) {
            // horray!
        }
        catch (final Exception ex) {
            ex.printStackTrace();
            fail("No exceptions allowed");
        }
    }
}
