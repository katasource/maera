package org.maera.plugin.factories;

import com.mockobjects.dynamic.Mock;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginParseException;

import java.io.File;

public class XmlDynamicPluginFactoryTest {

    @Test(expected = PluginParseException.class)
    public void testCreateBadXml() {
        XmlDynamicPluginFactory factory = new XmlDynamicPluginFactory("foo");
        Mock mockModuleDescriptorFactory = new Mock(ModuleDescriptorFactory.class);

        Mock mockArtifact = new Mock(PluginArtifact.class);
        mockArtifact.expectAndReturn("toFile", new File("sadfasdf"));
        factory.create((PluginArtifact) mockArtifact.proxy(), (ModuleDescriptorFactory) mockModuleDescriptorFactory.proxy());
    }
}
