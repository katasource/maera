package org.maera.plugin.parsers;

import org.maera.plugin.PluginParseException;

import java.io.InputStream;

/**
 * A factory for creating descriptor parsers.
 *
 * @see DescriptorParser
 * @see XmlDescriptorParserFactory
 */
public interface DescriptorParserFactory {
    /**
     * Creates a new {@link DescriptorParser} for getting plugin descriptor information
     * from the provided source data.
     *
     * @param source          the stream of data which represents the descriptor. The stream will
     *                        only be read once, so it need not be resettable.
     * @param applicationKeys The list of application keys to match for module descriptors
     * @return an instance of the descriptor parser tied to this InputStream
     * @throws PluginParseException if there was a problem creating the descriptor parser
     *                              due to an invalid source stream.
     */
    DescriptorParser getInstance(InputStream source, String... applicationKeys) throws PluginParseException;
}
