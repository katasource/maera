package org.maera.plugin.factories;

import com.mockobjects.dynamic.Mock;
import junit.framework.TestCase;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginParseException;

import java.io.File;

public class TestXmlDynamicPluginFactory extends TestCase {
    public void testCreateBadXml() {
        XmlDynamicPluginFactory factory = new XmlDynamicPluginFactory("foo");
        Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);
        try {
            Mock mockArtifact = new Mock(PluginArtifact.class);
            mockArtifact.expectAndReturn("toFile", new File("sadfasdf"));
            factory.create((PluginArtifact) mockArtifact.proxy(), (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy());
            fail("Should have thrown an exception");
        } catch (PluginParseException ex) {
            // horray!
        } catch (Exception ex) {
            ex.printStackTrace();
            fail("No exceptions allowed");
        }
    }
}
