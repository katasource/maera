package org.maera.plugin.servlet;

import org.maera.plugin.webresource.PluginResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.TreeMap;


/**
 * A downloadable plugin resource, as described here: http://confluence.atlassian.com/display/JIRA/Downloadable+plugin+resource
 * It supports the download of single plugin resources as well as batching.
 * <p/>
 * <p/>
 * The URL that it parses for a single resource looks like this: <br>
 * <code>{server root}/download/resources/{plugin key}:{module key}/{resource name}</code>
 * <p/>
 * The URL that it parses for a batch looks like this: <br>
 * <code>{server root}/download/batch/{plugin key}:{module key}/all.css?ieonly=true</code>
 */
public class PluginResourceDownload implements DownloadStrategy {
    private static final Logger log = LoggerFactory.getLogger(PluginResourceDownload.class);
    private String characterEncoding = "UTF-8"; // default to sensible encoding
    private PluginResourceLocator pluginResourceLocator;
    private ContentTypeResolver contentTypeResolver;

    public PluginResourceDownload() {
    }

    public PluginResourceDownload(PluginResourceLocator pluginResourceLocator, ContentTypeResolver contentTypeResolver, String characterEncoding) {
        this.characterEncoding = characterEncoding;
        this.pluginResourceLocator = pluginResourceLocator;
        this.contentTypeResolver = contentTypeResolver;
    }

    public boolean matches(String urlPath) {
        return pluginResourceLocator.matches(urlPath);
    }

    public void serveFile(HttpServletRequest request, HttpServletResponse response) throws DownloadException {
        try {
            String requestUri = URLDecoder.decode(request.getRequestURI(), characterEncoding);
            DownloadableResource downloadableResource = pluginResourceLocator.getDownloadableResource(requestUri, getQueryParameters(request));

            if (downloadableResource == null) {
                log.info("Could not locate resource: " + request.getRequestURI());
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (downloadableResource.isResourceModified(request, response)) {
                log.info("Plugin Resource has been modified since plugin was loaded. Skipping: " + requestUri);
                return;
            }

            String contentType = getContentType(requestUri, downloadableResource);
            if (contentType != null) {
                response.setContentType(contentType);
            }
            downloadableResource.serveResource(request, response);
        }
        catch (IOException e) {
            throw new DownloadException(e);
        }
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setContentTypeResolver(ContentTypeResolver contentTypeResolver) {
        this.contentTypeResolver = contentTypeResolver;
    }

    public void setPluginResourceLocator(PluginResourceLocator pluginResourceLocator) {
        this.pluginResourceLocator = pluginResourceLocator;
    }

    /**
     * Gets the content type for the resource. If the downloadable resource does not specify one, look one up
     * using the {@link ContentTypeResolver}.
     */
    private String getContentType(String requestUri, DownloadableResource downloadableResource) {
        String contentType = downloadableResource.getContentType();
        if (contentType == null) {
            return contentTypeResolver.getContentType(requestUri);
        }

        return contentType;
    }

    /**
     * Returns a Map of query parameters from the request. If there are multiple values for the same
     * query parameter, the first value is used.
     *
     * @see {@link javax.servlet.ServletRequest#getParameterMap()}
     */
    private Map<String, String> getQueryParameters(HttpServletRequest request) {
        Map<String, String> result = new TreeMap<String, String>();
        Map<String, String[]> parameters = request.getParameterMap();

        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0)
                result.put(entry.getKey(), entry.getValue()[0]);
        }

        return result;
    }
}

