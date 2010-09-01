package org.maera.plugin.osgi.factory.transform.stage;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;
import org.apache.commons.lang.StringUtils;
import org.maera.plugin.PluginInformation;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.osgi.factory.OsgiPlugin;
import org.maera.plugin.osgi.factory.transform.PluginTransformationException;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.TransformStage;
import org.maera.plugin.osgi.factory.transform.model.SystemExports;
import org.maera.plugin.osgi.util.OsgiHeaderUtil;
import org.maera.plugin.parsers.XmlDescriptorParser;
import org.maera.plugin.util.PluginUtils;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

/**
 * Generates an OSGi manifest if not already defined.  Should be the last stage.
 *
 * @since 0.1
 */
public class GenerateManifestStage implements TransformStage {
    
    private final int SPRING_TIMEOUT = PluginUtils.getDefaultEnablingWaitPeriod();
    private final String SPRING_CONTEXT_DEFAULT = "*;" + SPRING_CONTEXT_TIMEOUT + SPRING_TIMEOUT;
    static Logger log = LoggerFactory.getLogger(GenerateManifestStage.class);
    public static final String SPRING_CONTEXT = "Spring-Context";
    private static final String SPRING_CONTEXT_TIMEOUT = "timeout:=";
    private static final String SPRING_CONTEXT_DELIM = ";";

    public void execute(final TransformContext context) throws PluginTransformationException {
        final Builder builder = new Builder();
        try {
            builder.setJar(context.getPluginFile());

            // We don't care about the modules, so we pass null
            final XmlDescriptorParser parser = new XmlDescriptorParser(context.getDescriptorDocument());

            if (isOsgiBundle(context.getManifest())) {
                if (context.getExtraImports().isEmpty()) {
                    final Manifest mf = builder.getJar().getManifest();
                    for (Map.Entry<String, String> entry : getRequiredOsgiHeaders(context, parser.getKey()).entrySet()) {
                        mf.getMainAttributes().putValue(entry.getKey(), entry.getValue());
                    }
                    validateOsgiVersionIsValid(mf);
                    writeManifestOverride(context, mf);
                    // skip any manifest manipulation by bnd
                    return;
                } else {
                    // Possibly necessary due to Spring XML creation
                    assertSpringAvailableIfRequired(context);
                    final String imports = addExtraImports(builder.getJar().getManifest().getMainAttributes().getValue(Constants.IMPORT_PACKAGE), context.getExtraImports());
                    builder.setProperty(Constants.IMPORT_PACKAGE, imports);

                    for (Map.Entry<String, String> entry : getRequiredOsgiHeaders(context, parser.getKey()).entrySet()) {
                        builder.setProperty(entry.getKey(), entry.getValue());
                    }
                    builder.mergeManifest(builder.getJar().getManifest());
                }
            } else {
                final PluginInformation info = parser.getPluginInformation();

                final Properties properties = new Properties();

                // Setup defaults
                for (Map.Entry<String, String> entry : getRequiredOsgiHeaders(context, parser.getKey()).entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                properties.put(Analyzer.BUNDLE_SYMBOLICNAME, parser.getKey());
                properties.put(Analyzer.IMPORT_PACKAGE, "*;resolution:=optional");

                // Don't export anything by default
                //properties.put(Analyzer.EXPORT_PACKAGE, "*");

                properties.put(Analyzer.BUNDLE_VERSION, info.getVersion());

                // remove the verbose Include-Resource entry from generated manifest
                properties.put(Analyzer.REMOVE_HEADERS, Analyzer.INCLUDE_RESOURCE);

                header(properties, Analyzer.BUNDLE_DESCRIPTION, info.getDescription());
                header(properties, Analyzer.BUNDLE_NAME, parser.getKey());
                header(properties, Analyzer.BUNDLE_VENDOR, info.getVendorName());
                header(properties, Analyzer.BUNDLE_DOCURL, info.getVendorUrl());

                // Scan for embedded jars
                final StringBuilder classpath = new StringBuilder();
                classpath.append(".");
                for (final JarEntry jarEntry : context.getPluginJarEntries()) {
                    if (jarEntry.getName().startsWith("META-INF/lib/") && jarEntry.getName().endsWith(".jar")) {
                        classpath.append(",").append(jarEntry.getName());
                    }
                }
                header(properties, Analyzer.BUNDLE_CLASSPATH, classpath.toString());

                // Process any bundle instructions in maera-plugin.xml
                properties.putAll(context.getBndInstructions());

                // Add extra imports to the imports list
                properties.put(Analyzer.IMPORT_PACKAGE, addExtraImports(properties.getProperty(Analyzer.IMPORT_PACKAGE), context.getExtraImports()));

                // Add extra exports to the exports list
                if (!properties.containsKey(Analyzer.EXPORT_PACKAGE)) {
                    properties.put(Analyzer.EXPORT_PACKAGE, StringUtils.join(context.getExtraExports(), ','));
                }
                builder.setProperties(properties);
            }

            builder.calcManifest();
            builder.getJar().close();
            final Jar jar = builder.build();
            final Manifest mf = jar.getManifest();

            enforceHostVersionsForUnknownImports(mf, context.getSystemExports());
            validateOsgiVersionIsValid(mf);

            writeManifestOverride(context, mf);
            jar.close();
        }
        catch (final Exception t) {
            throw new PluginParseException("Unable to process plugin to generate OSGi manifest", t);
        } finally {
            builder.getJar().close();
            builder.close();
        }

    }

    private Map<String, String> getRequiredOsgiHeaders(TransformContext context, String pluginKey) {
        Map<String, String> props = new HashMap<String, String>();
        props.put(OsgiPlugin.MAERA_PLUGIN_KEY, pluginKey);
        String springHeader = getDesiredSpringContextValue(context);
        if (springHeader != null) {
            props.put(SPRING_CONTEXT, springHeader);
        }
        return props;
    }

    private String getDesiredSpringContextValue(TransformContext context) {
        // Check for the explicit context value
        final String header = context.getManifest().getMainAttributes().getValue(SPRING_CONTEXT);
        if (header != null) {
            return ensureDefaultTimeout(header);
        }

        // Check for the spring files, as the default header value looks here
        if (context.getPluginArtifact().doesResourceExist("META-INF/spring/") ||
                context.shouldRequireSpring() ||
                context.getDescriptorDocument() != null) {
            return SPRING_CONTEXT_DEFAULT;
        }
        return null;
    }

    private String ensureDefaultTimeout(final String header) {
        final boolean noTimeOutSpecified = StringUtils.isEmpty(System.getProperty(PluginUtils.MAERA_PLUGINS_ENABLE_WAIT));

        if (noTimeOutSpecified) {
            return header;
        } else {
            StringBuffer headerBuf;
            //Override existing timeout
            if (header.indexOf(SPRING_CONTEXT_TIMEOUT) != -1) {
                StringTokenizer tokenizer = new StringTokenizer(header, SPRING_CONTEXT_DELIM);
                headerBuf = new StringBuffer();
                while (tokenizer.hasMoreElements()) {
                    String directive = (String) tokenizer.nextElement();
                    if (directive.startsWith(SPRING_CONTEXT_TIMEOUT)) {
                        directive = SPRING_CONTEXT_TIMEOUT + SPRING_TIMEOUT;
                    }
                    headerBuf.append(directive);
                    if (tokenizer.hasMoreElements()) {
                        headerBuf.append(SPRING_CONTEXT_DELIM);
                    }
                }
            } else {
                //Append new timeout
                headerBuf = new StringBuffer(header);
                headerBuf.append(SPRING_CONTEXT_DELIM + SPRING_CONTEXT_TIMEOUT + SPRING_TIMEOUT);
            }
            return headerBuf.toString();
        }
    }

    private void validateOsgiVersionIsValid(Manifest mf) {
        String version = mf.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
        try {
            if (Version.parseVersion(version) == Version.emptyVersion) {
                // we still consider an empty version to be bad
                throw new IllegalArgumentException();
            }
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Plugin version '" + version + "' is required and must be able to be " +
                    "parsed as an OSGi version - MAJOR.MINOR.MICRO.QUALIFIER");
        }
    }

    private void writeManifestOverride(final TransformContext context, final Manifest mf)
            throws IOException {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mf.write(bout);
        context.getFileOverrides().put("META-INF/MANIFEST.MF", bout.toByteArray());
    }

    /**
     * Scans for any imports with no version specified and locks them into the specific version exported by the host
     * container
     *
     * @param manifest The manifest to read and manipulate
     * @param exports  The list of host exports
     */
    private void enforceHostVersionsForUnknownImports(final Manifest manifest, final SystemExports exports) {
        final String origImports = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
        if (origImports != null) {
            final StringBuilder imports = new StringBuilder();
            final Map<String, Map<String, String>> header = OsgiHeaderUtil.parseHeader(origImports);
            for (final Map.Entry<String, Map<String, String>> pkgImport : header.entrySet()) {
                String imp = null;
                if (pkgImport.getValue().isEmpty()) {
                    final String export = exports.getFullExport(pkgImport.getKey());
                    if (!export.equals(imp)) {
                        imp = export;
                    }

                }
                if (imp == null) {
                    imp = OsgiHeaderUtil.buildHeader(pkgImport.getKey(), pkgImport.getValue());
                }
                imports.append(imp);
                imports.append(",");
            }
            if (imports.length() > 0) {
                imports.deleteCharAt(imports.length() - 1);
            }

            manifest.getMainAttributes().putValue(Constants.IMPORT_PACKAGE, imports.toString());
        }
    }

    private boolean isOsgiBundle(final Manifest manifest) throws IOException {
        return manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) != null;
    }

    private String addExtraImports(String imports, final List<String> extraImports) {
        final StringBuilder referrers = new StringBuilder();
        for (final String imp : extraImports) {
            referrers.append(imp).append(",");
        }

        if (imports != null && imports.length() > 0) {
            imports = referrers + imports;
        } else {
            imports = referrers.toString();
        }
        return imports;
    }

    private static void header(final Properties properties, final String key, final Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Collection && ((Collection) value).isEmpty()) {
            return;
        }

        properties.put(key, value.toString().replaceAll("[\r\n]", ""));
    }

    private void assertSpringAvailableIfRequired(final TransformContext context) {
        if (context.shouldRequireSpring()) {
            final String header = context.getManifest().getMainAttributes().getValue(SPRING_CONTEXT);
            if (header == null) {
                log.debug("The Spring Manifest header 'Spring-Context' is missing in jar '" +
                        context.getPluginArtifact().toString() + "'.  If you experience any problems, please add it and set it to '" +
                        SPRING_CONTEXT_DEFAULT + "'");
            } else if (!header.contains(";timeout:=" + SPRING_TIMEOUT)) {
                log.warn("The Spring Manifest header in jar '" + context.getPluginArtifact().toString() + "' isn't " +
                        "set for a " + SPRING_TIMEOUT + " second timeout waiting for  dependencies.  " +
                        "Please add ';timeout:=" + SPRING_TIMEOUT + "'");
            }
        }
    }

}
