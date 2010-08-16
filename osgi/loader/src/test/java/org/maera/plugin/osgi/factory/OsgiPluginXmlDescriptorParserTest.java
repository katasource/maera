package org.maera.plugin.osgi.factory;

import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.junit.Test;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.PluginParseException;

import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class OsgiPluginXmlDescriptorParserTest {

    @Test
    public void testCreateModuleDescriptor() throws PluginParseException, IllegalAccessException, ClassNotFoundException, InstantiationException {
        OsgiPluginXmlDescriptorParser parser = new OsgiPluginXmlDescriptorParser(new ByteArrayInputStream("<foo/>".getBytes()), null);

        ModuleDescriptor desc = mock(ModuleDescriptor.class);
        when(desc.getKey()).thenReturn("foo");
        ModuleDescriptorFactory factory = mock(ModuleDescriptorFactory.class);
        when(factory.getModuleDescriptor("foo")).thenReturn(desc);

        OsgiPlugin plugin = mock(OsgiPlugin.class);
        Element fooElement = new DefaultElement("foo");
        fooElement.addAttribute("key", "bob");
        assertNotNull(parser.createModuleDescriptor(plugin, fooElement, factory));
        verify(plugin).addModuleDescriptorElement("foo", fooElement);
    }
}
