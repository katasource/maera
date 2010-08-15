package org.maera.plugin.osgi.factory;

import org.apache.commons.lang.Validate;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.parsers.DescriptorParser;
import org.maera.plugin.parsers.DescriptorParserFactory;

import java.io.InputStream;

/**
 * Descriptor parser factory that creates parsers for Osgi plugins.  Must only be used with {@link OsgiPlugin} instances.
 *
 * @since 2.1.2
 */
public class OsgiPluginXmlDescriptorParserFactory implements DescriptorParserFactory {
    /**
     * Gets an instance that filters the modules "component", "component-import", "module-type", "bean", and "spring"
     *
     * @param source The descriptor source
     * @return The parser
     * @throws PluginParseException
     */
    public DescriptorParser getInstance(final InputStream source, final String... applicationKeys) throws PluginParseException {
        Validate.notNull(source, "The descriptor source must not be null");
        return new OsgiPluginXmlDescriptorParser(source, applicationKeys);
    }
}