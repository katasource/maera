package org.maera.plugin.webresource;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.servlet.DownloadException;
import org.maera.plugin.servlet.DownloadableResource;
import org.maera.plugin.webresource.transformer.AbstractStringTransformedDownloadableResource;
import org.maera.plugin.webresource.transformer.WebResourceTransformer;
import org.maera.plugin.webresource.transformer.WebResourceTransformerModuleDescriptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestWebResourceTransformation extends TestCase {
    public void testMatches() throws DocumentException {
        WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                        "<transformer key=\"foo\" />\n" +
                        "</transformation>").getRootElement());
        ResourceLocation loc = mock(ResourceLocation.class);
        when(loc.getName()).thenReturn("foo.js");
        assertTrue(trans.matches(loc));
    }

    public void testNotMatches() throws DocumentException {
        WebResourceTransformation trans = new WebResourceTransformation(DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                        "<transformer key=\"foo\" />\n" +
                        "</transformation>").getRootElement());
        ResourceLocation loc = mock(ResourceLocation.class);
        when(loc.getName()).thenReturn("foo.cs");
        assertFalse(trans.matches(loc));
    }

    public void testNoExtension() throws DocumentException {
        try {
            new WebResourceTransformation(DocumentHelper.parseText(
                    "<transformation>\n" +
                            "<transformer key=\"foo\" />\n" +
                            "</transformation>").getRootElement());
            fail("Should have forced extension");
        }
        catch (IllegalArgumentException ex) {
            // pass
        }
    }

    public void testTransformDownloadableResource() throws DocumentException {
        Element element = DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                        "<transformer key=\"foo\" />\n" +
                        "</transformation>").getRootElement();
        WebResourceTransformation trans = new WebResourceTransformation(element);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        WebResourceTransformerModuleDescriptor descriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(descriptor.getKey()).thenReturn("foo");
        WebResourceTransformer transformer = mock(WebResourceTransformer.class);
        when(descriptor.getModule()).thenReturn(transformer);
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceTransformerModuleDescriptor.class)).thenReturn(
                Arrays.asList(descriptor));
        ResourceLocation loc = mock(ResourceLocation.class);
        when(loc.getName()).thenReturn("foo.js");

        DownloadableResource originalResource = mock(DownloadableResource.class);
        DownloadableResource transResource = mock(DownloadableResource.class);
        when(transformer.transform(element.element("transformer"), loc, "", originalResource)).thenReturn(transResource);

        DownloadableResource testResource = trans.transformDownloadableResource(pluginAccessor, originalResource, loc, "");
        assertEquals(transResource, testResource);
    }

    public void testTransformTwoDownloadableResources() throws DocumentException, DownloadException {
        Element element = DocumentHelper.parseText(
                "<transformation extension=\"js\">\n" +
                        "<transformer key=\"foo\" />\n" +
                        "<transformer key=\"bar\" />\n" +
                        "</transformation>").getRootElement();
        WebResourceTransformation trans = new WebResourceTransformation(element);
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);

        WebResourceTransformerModuleDescriptor fooDescriptor = createTransformer("foo");
        WebResourceTransformerModuleDescriptor barDescriptor = createTransformer("bar");
        when(pluginAccessor.getEnabledModuleDescriptorsByClass(WebResourceTransformerModuleDescriptor.class)).thenReturn(
                Arrays.asList(
                        fooDescriptor,
                        barDescriptor));
        ResourceLocation loc = mock(ResourceLocation.class);
        when(loc.getName()).thenReturn("foo.js");

        DownloadableResource originalResource = new StringDownloadableResource("resource");

        DownloadableResource testResource = trans.transformDownloadableResource(pluginAccessor, originalResource, loc, "");
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        testResource.streamResource(bout);
        assertEquals("bar: foo: resource", new String(bout.toByteArray()));
    }

    private WebResourceTransformerModuleDescriptor createTransformer(String key) {
        WebResourceTransformerModuleDescriptor descriptor = mock(WebResourceTransformerModuleDescriptor.class);
        when(descriptor.getKey()).thenReturn(key);
        WebResourceTransformer fooTransformer = new PrefixTransformer(key + ": ");
        when(descriptor.getModule()).thenReturn(fooTransformer);
        return descriptor;
    }

    private static class PrefixTransformer implements WebResourceTransformer {

        private final String prefix;

        public PrefixTransformer(String prefix) {
            this.prefix = prefix;
        }

        public DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource) {
            return new AbstractStringTransformedDownloadableResource(nextResource) {
                protected String transform(String originalContent) {
                    return prefix + originalContent;
                }
            };
        }
    }

    private static class StringDownloadableResource implements DownloadableResource {
        final String value;

        public StringDownloadableResource(String value) {
            this.value = value;
        }

        public boolean isResourceModified(HttpServletRequest request, HttpServletResponse response) {
            return false;
        }

        public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException {

        }

        public void streamResource(OutputStream out) throws DownloadException {
            try {
                IOUtils.write(value, out);
            }
            catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        public String getContentType() {
            return "text/plain";
        }
    }

}
