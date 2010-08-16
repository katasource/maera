package org.maera.plugin.elements;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import static org.junit.Assert.*;

public class ResourceDescriptorTest {

    @Test
    public void testBasicResource() throws DocumentException {
        Document document = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"/foo/bar.vm\" />");
        ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());

        assertEquals("velocity", descriptor.getType());
        assertEquals("view", descriptor.getName());
        assertEquals("/foo/bar.vm", descriptor.getLocation());
        assertNull(descriptor.getContent());
    }

    @Test
    public void testEquality() throws DocumentException {
        Document velViewDoc = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\">the content</resource>");
        Document velViewDoc2 = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"foo\" />");
        Document velViewDoc3 = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"foo\"><param name=\"narrator\">Tyler Durden</param></resource>");
        Document velEditDoc = DocumentHelper.parseText("<resource type=\"velocity\" name=\"edit\">the content</resource>");
        Document fooEditDoc = DocumentHelper.parseText("<resource type=\"foo\" name=\"edit\">the content</resource>");
        ResourceDescriptor velViewResource = new ResourceDescriptor(velViewDoc.getRootElement());
        ResourceDescriptor velViewResource2 = new ResourceDescriptor(velViewDoc2.getRootElement());
        ResourceDescriptor velViewResource3 = new ResourceDescriptor(velViewDoc3.getRootElement());
        ResourceDescriptor velEditResource = new ResourceDescriptor(velEditDoc.getRootElement());
        ResourceDescriptor fooEditResource = new ResourceDescriptor(fooEditDoc.getRootElement());

        assertEquals(velViewResource, velViewResource);
        assertEquals(velViewResource, velViewResource2);
        assertEquals(velViewResource, velViewResource3);
        assertEquals(velViewResource2, velViewResource);
        assertEquals(velViewResource3, velViewResource);
        assertFalse(velViewResource.equals(velEditResource));
        assertFalse(velEditResource.equals(velViewResource));
        assertFalse(fooEditResource.equals(velEditResource));
    }

    @Test
    public void testEquals() {
        Element desc = DocumentHelper.createElement("foo");
        desc.addAttribute("name", "foo");

        ResourceDescriptor descriptor = new ResourceDescriptor(desc);
        assertTrue(descriptor.equals(descriptor));
        assertTrue(descriptor.equals(new ResourceDescriptor(desc)));
    }

    @Test
    public void testEqualsWithNullNameAndType() {
        Element e = DocumentHelper.createElement("foo");
        e.addAttribute("namePattern", "foo");
        e.addAttribute("location", "/foo/");
        ResourceDescriptor desc = new ResourceDescriptor(e);

        ResourceDescriptor desc2 = new ResourceDescriptor(e);
        assertTrue(desc.equals(desc2));
    }

    @Test
    public void testEqualsWithNullType() {
        Element e = DocumentHelper.createElement("foo");
        e.addAttribute("name", "foo");
        e.addAttribute("location", "/foo/");
        ResourceDescriptor desc = new ResourceDescriptor(e);

        e.addAttribute("type", "foo");
        ResourceDescriptor desc2 = new ResourceDescriptor(e);
        assertFalse(desc.equals(desc2));

        desc = new ResourceDescriptor(e);

        e.addAttribute("type", null);
        desc2 = new ResourceDescriptor(e);
        assertFalse(desc.equals(desc2));
    }

    @Test
    public void testGetResourceLocationForNameForSingleResource() throws DocumentException {
        Document xml = DocumentHelper.parseText("<resource type=\"foo\" name=\"bob.jpg\" location=\"path/to/builders/\"/>");
        ResourceDescriptor rd = new ResourceDescriptor(xml.getRootElement());
        final ResourceLocation location = rd.getResourceLocationForName("builders/bob.jpg");
        assertEquals(rd.getLocation(), location.getLocation());
        assertEquals(rd.getType(), location.getType());
        assertEquals(rd.getName(), location.getName());
    }

    @Test
    public void testHashcode() {
        Element desc = DocumentHelper.createElement("foo");
        desc.addAttribute("name", "foo");
        desc.addAttribute("type", "bar");
        assertNotNull(new ResourceDescriptor(desc).hashCode());
        desc.addAttribute("type", null);
        assertNotNull(new ResourceDescriptor(desc).hashCode());
        desc.addAttribute("name", null);
        desc.addAttribute("namePattern", "foo");
        desc.addAttribute("location", "bar");
    }

    @Test
    public void testMultiResourceDescriptor() throws DocumentException {
        Document multiResources = DocumentHelper.parseText("<resource type=\"foo\" namePattern=\".*\\.jpg\" location=\"xxx/\"/>");
        ResourceDescriptor rd = new ResourceDescriptor(multiResources.getRootElement());
        final ResourceLocation location = rd.getResourceLocationForName("fred.jpg");
        assertEquals("foo", location.getType());
        assertEquals("fred.jpg", location.getName());
        assertEquals("xxx/", location.getLocation());
        try {
            rd.getResourceLocationForName("fred.gif");
            fail();
        }
        catch (RuntimeException re) {
            // expect to fail when name doesn't match pattern
        }
    }

    @Test
    public void testResourceWithContent() throws DocumentException {
        Document document = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\">the content</resource>");
        ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());

        assertEquals("velocity", descriptor.getType());
        assertEquals("view", descriptor.getName());
        assertNull(descriptor.getLocation());
        assertEquals("the content", descriptor.getContent());
    }

    @Test
    public void testResourceWithParameters() throws DocumentException {
        Document document = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\" location=\"/foo/bar.vm\">" +
                "<param name=\"attribute\" value=\"20\"/>" +
                "<param name=\"content\">fish</param></resource>");
        ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());

        assertEquals("velocity", descriptor.getType());
        assertEquals("view", descriptor.getName());
        assertEquals("/foo/bar.vm", descriptor.getLocation());
        assertNull(descriptor.getContent());
        assertEquals("20", descriptor.getParameter("attribute"));
        assertEquals("fish", descriptor.getParameter("content"));
    }

    @Test
    public void testResourceWithParametersAndContent() throws DocumentException {
        Document document = DocumentHelper.parseText("<resource type=\"velocity\" name=\"view\">" +
                "<param name=\"attribute\" value=\"20\"/>" +
                "<param name=\"content\">fish</param>" +
                "This is the content.</resource>");
        ResourceDescriptor descriptor = new ResourceDescriptor(document.getRootElement());

        assertEquals("velocity", descriptor.getType());
        assertEquals("view", descriptor.getName());
        assertNull(descriptor.getLocation());
        assertEquals("This is the content.", descriptor.getContent());
        assertEquals("20", descriptor.getParameter("attribute"));
        assertEquals("fish", descriptor.getParameter("content"));
    }
}
