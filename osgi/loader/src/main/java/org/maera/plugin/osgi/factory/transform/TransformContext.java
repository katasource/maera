package org.maera.plugin.osgi.factory.transform;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.dom4j.Document;
import org.dom4j.Element;
import org.maera.plugin.PluginArtifact;
import org.maera.plugin.PluginParseException;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.factory.transform.model.ComponentImport;
import org.maera.plugin.osgi.factory.transform.model.SystemExports;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.parsers.XmlDescriptorParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * The transform context containing any configuration necessary to enact a JAR transformation
 *
 * @since 0.1
 */
public class TransformContext {
    
//    private final JarFile pluginJar;
    private final Manifest manifest;
    private final List<HostComponentRegistration> regs;
    private final Map<String, byte[]> fileOverrides;
    private final Map<String, String> bndInstructions;
    private final Document descriptorDocument;
    private final List<String> extraImports;
    private final List<String> extraExports;
    private final PluginArtifact pluginArtifact;
    private final Map<String, ComponentImport> componentImports;
    private final SystemExports systemExports;
    private final Set<String> applicationKeys;
    private boolean shouldRequireSpring = false;
    private final OsgiContainerManager osgiContainerManager;
    private Set<HostComponentRegistration> requiredHostComponents;

    public TransformContext(final List<HostComponentRegistration> regs, final SystemExports systemExports,
                            final PluginArtifact pluginArtifact, final Set<String> applicationKeys, final String descriptorPath,
                            final OsgiContainerManager osgiContainerManager) {
        this.osgiContainerManager = osgiContainerManager;
        Validate.notNull(pluginArtifact, "The plugin artifact must be specified");
        Validate.notNull(descriptorPath, "The plugin descriptor path must be specified");
        Validate.notNull(systemExports, "The system exports must be specified");

        this.regs = regs;
        this.systemExports = systemExports;
        this.pluginArtifact = pluginArtifact;
        this.applicationKeys = (applicationKeys == null ? Collections.<String>emptySet() : applicationKeys);

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(pluginArtifact.toFile());
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                this.manifest = new Manifest();
            } else {
                this.manifest = manifest;
            }
        }
        catch (final IOException e) {
            throw new IllegalArgumentException("File must be a jar", e);
        } finally {
            closeJarQuietly(jarFile);
        }
        fileOverrides = new HashMap<String, byte[]>();
        bndInstructions = new HashMap<String, String>();
        this.descriptorDocument = retrieveDocFromJar(pluginArtifact, descriptorPath);
        this.extraImports = new ArrayList<String>();
        this.extraExports = new ArrayList<String>();

        this.componentImports = Collections.unmodifiableMap(parseComponentImports(descriptorDocument));
        requiredHostComponents = new HashSet<HostComponentRegistration>();
    }

    private Map<String, ComponentImport> parseComponentImports(final Document descriptorDocument) {
        final Map<String, ComponentImport> componentImports = new HashMap<String, ComponentImport>();
        final List<Element> elements = descriptorDocument.getRootElement().elements("component-import");
        for (final Element component : elements) {
            final ComponentImport ci = new ComponentImport(component);
            componentImports.put(ci.getKey(), ci);
        }
        return componentImports;
    }

    private Document retrieveDocFromJar(final PluginArtifact pluginArtifact, final String descriptorPath) throws PluginTransformationException {
        Document document;
        InputStream stream = null;
        try {
            stream = pluginArtifact.getResourceAsStream(descriptorPath);
            if (stream == null) {
                throw new PluginTransformationException("Unable to access descriptor " + descriptorPath);
            }
            final DocumentExposingDescriptorParser parser = new DocumentExposingDescriptorParser(stream);
            document = parser.getDocument();
        }
        finally {
            IOUtils.closeQuietly(stream);
        }
        return document;
    }

    public File getPluginFile() {
        return pluginArtifact.toFile();
    }

    public PluginArtifact getPluginArtifact() {
        return pluginArtifact;
    }

    public List<HostComponentRegistration> getHostComponentRegistrations() {
        return regs;
    }

    public Map<String, byte[]> getFileOverrides() {
        return fileOverrides;
    }

    public Map<String, String> getBndInstructions() {
        return bndInstructions;
    }

    public Document getDescriptorDocument() {
        return descriptorDocument;
    }

    public Manifest getManifest() {
        return manifest;
    }

    public List<String> getExtraImports() {
        return extraImports;
    }

    public List<String> getExtraExports() {
        return extraExports;
    }

    public Map<String, ComponentImport> getComponentImports() {
        return componentImports;
    }

    public SystemExports getSystemExports() {
        return systemExports;
    }

    public Set<String> getApplicationKeys() {
        return applicationKeys;
    }

    public boolean shouldRequireSpring() {
        return shouldRequireSpring;
    }

    public void setShouldRequireSpring(final boolean shouldRequireSpring) {
        this.shouldRequireSpring = shouldRequireSpring;
        if (shouldRequireSpring) {
            getFileOverrides().put("META-INF/spring/", new byte[0]);
        }

    }

    public OsgiContainerManager getOsgiContainerManager() {
        return osgiContainerManager;
    }

    private static class DocumentExposingDescriptorParser extends XmlDescriptorParser {
        /**
         * @throws org.maera.plugin.PluginParseException
         *          if there is a problem reading the descriptor from the XML {@link java.io.InputStream}.
         */
        public DocumentExposingDescriptorParser(final InputStream source) throws PluginParseException {
            // A null application key is fine here as we are only interested in the parsed document
            super(source, null);
        }

        @Override
        public Document getDocument() {
            return super.getDocument();
        }
    }

    public List<JarEntry> getPluginJarEntries() {
        final List<JarEntry> list = new ArrayList<JarEntry>();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(pluginArtifact.toFile());
            final Enumeration<JarEntry> entries = jarFile.entries();
            for (final Enumeration<JarEntry> e = entries; e.hasMoreElements();) {
                final JarEntry entry = e.nextElement();
                list.add(entry);
            }
            return list;
        }
        catch (final IOException e) {
            throw new IllegalArgumentException("File must be a jar", e);
        } finally {
            closeJarQuietly(jarFile);
        }
    }

    public Object getPluginJarEntry(final String path) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(pluginArtifact.toFile());
            return jarFile.getEntry(path);
        }
        catch (final IOException e) {
            throw new IllegalArgumentException("File must be a jar", e);
        } finally {
            closeJarQuietly(jarFile);
        }
    }

    private void closeJarQuietly(final JarFile jarFile) {
        if (jarFile != null)
            try {
                jarFile.close();
            } catch (final IOException e) {
                // ignore
            }
    }

    public void addRequiredHostComponent(HostComponentRegistration hostComponent) {
        requiredHostComponents.add(hostComponent);
    }

    public Set<HostComponentRegistration> getRequiredHostComponents() {
        return requiredHostComponents;
    }
}
