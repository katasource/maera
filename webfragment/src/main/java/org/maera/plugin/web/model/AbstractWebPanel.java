package org.maera.plugin.web.model;

import com.google.common.base.Preconditions;
import org.maera.plugin.Plugin;
import org.maera.plugin.PluginAccessor;
import org.maera.plugin.web.renderer.RendererException;
import org.maera.plugin.web.renderer.StaticWebPanelRenderer;
import org.maera.plugin.web.renderer.WebPanelRenderer;

/**
 * @since 2.5.0
 */
public abstract class AbstractWebPanel implements WebPanel {
    private final PluginAccessor pluginAccessor;
    protected Plugin plugin;
    private String resourceType;

    protected AbstractWebPanel(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = Preconditions.checkNotNull(resourceType);
    }

    protected final WebPanelRenderer getRenderer() {
        if (StaticWebPanelRenderer.RESOURCE_TYPE.equals(resourceType)) {
            return StaticWebPanelRenderer.RENDERER;
        } else {
            for (WebPanelRenderer webPanelRenderer : pluginAccessor.getEnabledModulesByClass(WebPanelRenderer.class)) {
                if (Preconditions.checkNotNull(resourceType).equals(webPanelRenderer.getResourceType())) {
                    return webPanelRenderer;
                }
            }
            throw new RendererException("No renderer found for resource type: " + resourceType);
        }
    }
}
