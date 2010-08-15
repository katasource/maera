package org.maera.plugin.web.model;

import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.web.renderer.WebPanelRenderer;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EmbeddedTemplateWebPanelTest extends TestCase {
    public void testGetHtml() {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(Collections.<WebPanelRenderer>emptyList());

        final EmbeddedTemplateWebPanel embeddedTemplateWebPanel = new EmbeddedTemplateWebPanel(accessorMock);
        embeddedTemplateWebPanel.setResourceType("static");
        embeddedTemplateWebPanel.setTemplateBody("body");

        assertEquals("body", embeddedTemplateWebPanel.getHtml(Collections.<String, Object>emptyMap()));
    }

    public void testUnsupportedResourceType() {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        final WebPanelRenderer renderer = mock(WebPanelRenderer.class);
        when(renderer.getResourceType()).thenReturn("velocity");
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(ImmutableList.of(renderer));

        final EmbeddedTemplateWebPanel embeddedTemplateWebPanel = new EmbeddedTemplateWebPanel(accessorMock);
        embeddedTemplateWebPanel.setResourceType("unsupported-type");
        embeddedTemplateWebPanel.setTemplateBody("body");

        final String result = embeddedTemplateWebPanel.getHtml(Collections.<String, Object>emptyMap());
        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error"));
    }
}
