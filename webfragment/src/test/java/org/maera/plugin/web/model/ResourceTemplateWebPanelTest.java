package org.maera.plugin.web.model;

import com.google.common.collect.ImmutableList;
import junit.framework.TestCase;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.web.renderer.WebPanelRenderer;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceTemplateWebPanelTest extends TestCase {
    public void testGetHtml() {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(Collections.<WebPanelRenderer>emptyList());
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getClassLoader()).thenReturn(this.getClass().getClassLoader());

        final ResourceTemplateWebPanel resourceTemplateWebPanel = new ResourceTemplateWebPanel(accessorMock);
        resourceTemplateWebPanel.setPlugin(plugin);
        resourceTemplateWebPanel.setResourceType("static");
        resourceTemplateWebPanel.setResourceFilename("ResourceTemplateWebPanelTest.txt");

        assertTrue(resourceTemplateWebPanel.getHtml(Collections.<String, Object>emptyMap())
                .contains("This file is used as web panel contents in unit tests."));
    }

    public void testUnsupportedResourceType() {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        final WebPanelRenderer renderer = mock(WebPanelRenderer.class);
        when(renderer.getResourceType()).thenReturn("velocity");
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(ImmutableList.of(renderer));
        final Plugin plugin = mock(Plugin.class);
        when(plugin.getClassLoader()).thenReturn(this.getClass().getClassLoader());

        final ResourceTemplateWebPanel resourceTemplateWebPanel = new ResourceTemplateWebPanel(accessorMock);
        resourceTemplateWebPanel.setPlugin(plugin);
        resourceTemplateWebPanel.setResourceType("unsupported-type");
        resourceTemplateWebPanel.setResourceFilename("ResourceTemplateWebPanelTest.txt");

        final String result = resourceTemplateWebPanel.getHtml(Collections.<String, Object>emptyMap());
        assertNotNull(result);
        assertTrue(result.toLowerCase().contains("error"));
    }
}
