package org.maera.plugin.servlet;

import org.maera.plugin.Plugin;
import org.maera.plugin.elements.ResourceLocation;

import javax.servlet.ServletContext;
import java.io.InputStream;

/**
 * A {@link DownloadableResource} that will serve the resource via the web application's {@link ServletContext}.
 */
public class DownloadableWebResource extends AbstractDownloadableResource {
    private final ServletContext servletContext;

    public DownloadableWebResource(Plugin plugin, ResourceLocation resourceLocation, String extraPath, ServletContext servletContext, boolean disableMinification) {
        super(plugin, resourceLocation, extraPath, disableMinification);
        this.servletContext = servletContext;
    }

    @Override
    protected InputStream getResourceAsStream(final String resourceLocation) {
        return servletContext.getResourceAsStream(resourceLocation);
    }
}
