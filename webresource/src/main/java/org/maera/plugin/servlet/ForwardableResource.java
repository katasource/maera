package org.maera.plugin.servlet;

import org.apache.commons.lang.StringUtils;
import org.maera.plugin.elements.ResourceLocation;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A DownloadableResource that simply forwards the request to the given location.
 * This should be used to reference dynamic resources available in the web application e.g dwr js files
 */
public class ForwardableResource implements DownloadableResource {
    private ResourceLocation resourceLocation;

    public ForwardableResource(ResourceLocation resourceLocation) {
        this.resourceLocation = resourceLocation;
    }

    public boolean isResourceModified(HttpServletRequest request, HttpServletResponse response) {
        return false;
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException {
        try {
            String type = getContentType();
            if (StringUtils.isNotBlank(type)) {
                response.setContentType(type); // this will be used if content-type is not set by the forward handler, e.g. for webapp content in Tomcat
            }
            request.getRequestDispatcher(getLocation()).forward(request, response);
        }
        catch (ServletException e) {
            throw new DownloadException(e.getMessage());
        }
        catch (IOException ioe) {
            throw new DownloadException(ioe.getMessage());
        }
    }

    /**
     * Not implemented by a <code>ForwardableResource</code>. The supplied OutputStream will not be modified.
     */
    public void streamResource(OutputStream out) {
        return;
    }

    public String getContentType() {
        return resourceLocation.getContentType();
    }

    protected String getLocation() {
        return resourceLocation.getLocation();
    }
}

