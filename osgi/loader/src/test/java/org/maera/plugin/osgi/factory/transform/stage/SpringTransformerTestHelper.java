package org.maera.plugin.osgi.factory.transform.stage;

import junit.framework.Assert;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.maera.plugin.JarPluginArtifact;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.osgi.container.OsgiContainerManager;
import org.maera.plugin.osgi.factory.transform.TransformContext;
import org.maera.plugin.osgi.factory.transform.TransformStage;
import org.maera.plugin.osgi.factory.transform.model.SystemExports;
import org.maera.plugin.osgi.hostcomponents.HostComponentRegistration;
import org.maera.plugin.test.PluginJarBuilder;
import org.osgi.framework.ServiceReference;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Executes a transformation stage and tests xpath expressions against it
 */
public class SpringTransformerTestHelper {

    public static Element transform(final TransformStage transformer, final Element pluginRoot, final String... xpaths) throws IOException, DocumentException {
        return transform(transformer, null, pluginRoot, xpaths);
    }

    public static Element transform(final TransformStage transformer, final List<HostComponentRegistration> regs, final Element pluginRoot, final String... xpaths) throws IOException, DocumentException {
        return transform(transformer, null, regs, pluginRoot, xpaths);
    }

    public static Element transform(final TransformStage transformer, File pluginJar, final List<HostComponentRegistration> regs, final Element pluginRoot, final String... xpaths) throws IOException, DocumentException {
        if (pluginJar == null) {
            final String swriter = elementToString(pluginRoot);
            pluginJar = new PluginJarBuilder().addResource(PluginAccessor.Descriptor.FILENAME, swriter).build();
        }
        Set<String> keys = new HashSet<String>(Arrays.asList("foo"));

        OsgiContainerManager osgiContainerManager = mock(OsgiContainerManager.class);
        when(osgiContainerManager.getRegisteredServices()).thenReturn(new ServiceReference[0]);

        final TransformContext context = new TransformContext(regs, SystemExports.NONE, new JarPluginArtifact(pluginJar), keys, PluginAccessor.Descriptor.FILENAME, osgiContainerManager);

        transformer.execute(context);

        byte[] content = null;
        for (byte[] entry : context.getFileOverrides().values()) {
            if (entry.length > 0) {
                content = entry;
            }
        }
        if (content == null) {
            return null;
        }
        final Document springDoc = DocumentHelper.parseText(new String(content));
        final Element springRoot = springDoc.getRootElement();

        for (final String xp : xpaths) {
            final XPath xpath = DocumentHelper.createXPath(xp);
            final Object obj = xpath.selectObject(springRoot);
            if (obj instanceof Node) {
                // test passed
            } else if (obj instanceof Boolean) {
                if (!((Boolean) obj).booleanValue()) {
                    printDocument(springDoc);
                    Assert.fail("Unable to match xpath: " + xp);
                }
            } else if (obj == null) {
                printDocument(springDoc);
                Assert.fail("Unable to match xpath: " + xp);
            } else {
                printDocument(springDoc);
                Assert.fail("Unexpected result:" + obj);
            }
        }
        return springRoot;
    }

    static String elementToString(Element pluginRoot)
            throws IOException {
        final StringWriter swriter = new StringWriter();
        final OutputFormat outformat = OutputFormat.createPrettyPrint();
        final XMLWriter writer = new XMLWriter(swriter, outformat);
        writer.write(pluginRoot.getDocument());
        writer.flush();
        return swriter.toString();
    }

    private static void printDocument(final Document springDoc) throws IOException {
        final OutputFormat outformat = OutputFormat.createPrettyPrint();
        final XMLWriter writer = new XMLWriter(System.out, outformat);
        writer.write(springDoc);
        writer.flush();
    }

}
