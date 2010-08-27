package org.maera.plugin.web.model;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.web.renderer.RendererException;
import org.maera.plugin.web.renderer.StaticWebPanelRenderer;
import org.maera.plugin.web.renderer.WebPanelRenderer;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractWebPanelTest {

    @Test
    public void testStaticRenderer() {
        AbstractWebPanel panel = new AbstractWebPanel(null) {

            public String getHtml(Map<String, Object> context) {
                final WebPanelRenderer renderer = getRenderer();
                assertEquals(renderer, StaticWebPanelRenderer.RENDERER);
                return null;
            }
        };

        panel.setResourceType(StaticWebPanelRenderer.RESOURCE_TYPE);
        panel.getHtml(null);
    }

    @Test
    public void testSupportedRendererType() {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        final WebPanelRenderer velocityRenderer = mock(WebPanelRenderer.class);
        when(velocityRenderer.getResourceType()).thenReturn("velocity");
        final WebPanelRenderer unsupportedRenderer = mock(WebPanelRenderer.class);
        when(unsupportedRenderer.getResourceType()).thenReturn("unsupported-type");
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(ImmutableList.of(unsupportedRenderer, velocityRenderer));

        AbstractWebPanel panel = new AbstractWebPanel(accessorMock) {

            public String getHtml(Map<String, Object> context) {
                final WebPanelRenderer webPanelRenderer = getRenderer();
                assertNotNull(webPanelRenderer);
                assertEquals(velocityRenderer, webPanelRenderer);
                return null;
            }
        };

        panel.setResourceType("velocity");
        panel.getHtml(null);
    }

    @Test
    public void testUnsupportedRendererType() {
        final PluginAccessor accessorMock = mock(PluginAccessor.class);
        when(accessorMock.getEnabledModulesByClass(WebPanelRenderer.class)).thenReturn(Collections.<WebPanelRenderer>emptyList());

        AbstractWebPanel panel = new AbstractWebPanel(accessorMock) {

            public String getHtml(Map<String, Object> context) {
                try {
                    getRenderer();
                    fail();
                }
                catch (RendererException re) {
                    // expected
                }
                return null;
            }
        };

        panel.setResourceType("unsupported-type");
        panel.getHtml(null);
    }
}
