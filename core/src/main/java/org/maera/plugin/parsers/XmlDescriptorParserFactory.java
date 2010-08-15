package org.maera.plugin.parsers;

import org.maera.plugin.PluginParseException;

import java.io.InputStream;

/**
 * Creates XML descriptor parser instances.
 *
 * @see XmlDescriptorParser
 * @see DescriptorParserFactory
 */
public class XmlDescriptorParserFactory implements DescriptorParserFactory {
    /**
     * @param source          the stream of data which represents the descriptor. The stream will
     *                        only be read once, so it need not be resettable.
     * @param applicationKeys the identifier of the current application to use to match modules, if specified.  Null to
     *                        match only modules with no application key.
     * @return
     * @throws PluginParseException
     */
    public DescriptorParser getInstance(InputStream source, String... applicationKeys) throws PluginParseException {
        return new XmlDescriptorParser(source, applicationKeys);
    }
}
