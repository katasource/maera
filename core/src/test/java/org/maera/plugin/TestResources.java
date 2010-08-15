package org.maera.plugin;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.maera.plugin.elements.ResourceDescriptor;
import org.maera.plugin.elements.ResourceLocation;

import java.util.List;

public class TestResources extends TestCase {
    private static final String RESOURCE_DOC = "<foo>" + "<resource type=\"velocity\" name=\"view\">the content</resource>" + "<resource type=\"velocity\" name=\"edit\" />"
            + "<resource type=\"image\" name=\"view\" />" + "</foo>";

    public void testMultipleResources() throws DocumentException, PluginParseException {
        final Resources resources = makeTestResources();

        final List descriptors = resources.getResourceDescriptors();
        assertEquals(3, descriptors.size());

        assertDescriptorMatches((ResourceDescriptor) descriptors.get(0), "velocity", "view");
        assertDescriptorMatches((ResourceDescriptor) descriptors.get(1), "velocity", "edit");
        assertDescriptorMatches((ResourceDescriptor) descriptors.get(2), "image", "view");
    }

    public void testGetResourceDescriptorsByType() throws DocumentException, PluginParseException {
        final Resources resources = makeTestResources();

        assertEquals(0, resources.getResourceDescriptors("blah").size());

        final List velocityResources = resources.getResourceDescriptors("velocity");
        assertEquals(2, velocityResources.size());

        assertDescriptorMatches((ResourceDescriptor) velocityResources.get(0), "velocity", "view");
        assertDescriptorMatches((ResourceDescriptor) velocityResources.get(1), "velocity", "edit");
    }

    public void testNullTypeThrows() throws PluginParseException, DocumentException {
        final Resources resources = makeTestResources();
        try {
            resources.getResourceDescriptors(null);
            fail("IllegalArgumentException expected");
        }
        catch (final IllegalArgumentException expected) {
        }
    }

    public void testGetResourceDescriptor() throws DocumentException, PluginParseException {
        final Resources resources = makeTestResources();

        assertNull(resources.getResourceLocation("image", "edit"));
        assertNull(resources.getResourceLocation("fish", "view"));
        assertNull(resources.getResourceLocation(null, "view"));
        assertNull(resources.getResourceLocation("image", null));

        assertLocationMatches(resources.getResourceLocation("image", "view"), "image", "view");

    }

    public void testMultipleResourceWithClashingKeysFail() throws DocumentException {
        final Document document = DocumentHelper.parseText("<foo>" + "<resource type=\"velocity\" name=\"view\">the content</resource>"
                + "<resource type=\"velocity\" name=\"view\" />" + "</foo>");

        try {

            Resources.fromXml(document.getRootElement());
            fail("Should have thrown exception about duplicate resources.");
        }
        catch (final PluginParseException e) {
            assertEquals("Duplicate resource with type 'velocity' and name 'view' found", e.getMessage());
        }
    }

    public void testParsingNullElementThrowsException() throws Exception {
        try {
            Resources.fromXml(null);
            fail("Expected exception when parsing null element");
        }
        catch (final IllegalArgumentException expected) {
        }
    }

    public void testEmptyResources() throws Exception {
        final Resources resources = Resources.EMPTY_RESOURCES;
        assertTrue("Empty resources should be empty", resources.getResourceDescriptors().isEmpty());
        assertTrue("Empty resources should be empty by type", resources.getResourceDescriptors("i18n").isEmpty());
        assertNull("Empty resources should return null for any resource", resources.getResourceLocation("i18n", "i18n.properties"));
    }

    private void assertLocationMatches(final ResourceLocation first, final String type, final String name) {
        assertEquals(type, first.getType());
        assertEquals(name, first.getName());
    }

    private void assertDescriptorMatches(final ResourceDescriptor first, final String type, final String name) {
        assertEquals(type, first.getType());
        assertEquals(name, first.getName());
    }

    private Resources makeTestResources() throws DocumentException, PluginParseException {
        final Document document = DocumentHelper.parseText(RESOURCE_DOC);
        return Resources.fromXml(document.getRootElement());
    }
}
