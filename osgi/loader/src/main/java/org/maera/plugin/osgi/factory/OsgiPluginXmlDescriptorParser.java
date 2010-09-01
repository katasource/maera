package org.maera.plugin.osgi.factory;

import org.apache.commons.lang.Validate;
import org.dom4j.Element;
import org.maera.plugin.ModuleDescriptor;
import org.maera.plugin.ModuleDescriptorFactory;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.parsers.XmlDescriptorParser;

import java.io.InputStream;

/**
 * Descriptor parser that handles special tasks for osgi plugins such as recording the
 * originating module descriptor elements.  Must only be used with {@link OsgiPlugin} instances.
 *
 * @since 0.1
 */
public class OsgiPluginXmlDescriptorParser extends XmlDescriptorParser {

    /**
     * @param source          The XML descriptor source
     * @param applicationKeys The application keys to limit modules to, null for only unspecified
     * @throws org.maera.plugin.PluginParseException
     *          if there is a problem reading the descriptor from the XML {@link java.io.InputStream}.
     */
    public OsgiPluginXmlDescriptorParser(final InputStream source, final String... applicationKeys) throws PluginParseException {
        super(source, applicationKeys);
        Validate.notNull(source, "The descriptor source must not be null");
    }

    /**
     * Passes module descriptor elements back to the {@link OsgiPlugin}
     *
     * @param plugin                  The plugin
     * @param element                 The module element
     * @param moduleDescriptorFactory The module descriptor factory
     * @return The module, or null if the module cannot be found
     * @throws PluginParseException
     */
    @Override
    protected ModuleDescriptor<?> createModuleDescriptor(final Plugin plugin, final Element element, final ModuleDescriptorFactory moduleDescriptorFactory) throws PluginParseException {
        final ModuleDescriptor<?> descriptor = super.createModuleDescriptor(plugin, element, moduleDescriptorFactory);
        final String key = (descriptor != null ? descriptor.getKey() : element.attributeValue("key"));
        ((OsgiPlugin) plugin).addModuleDescriptorElement(key, element);
        return descriptor;
    }
}
