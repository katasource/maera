package org.maera.plugin.webresource;

import org.maera.plugin.servlet.DownloadException;
import org.maera.plugin.servlet.DownloadableResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.Map;

import static org.maera.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static org.maera.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;
import static org.maera.plugin.util.EfficientStringUtils.endsWith;

/**
 * Creates a batch of all like-typed resources that are declared as "super-batch="true"" in their plugin
 * definitions.
 * <p/>
 * The URL for batch resources is /download/superbatch/&lt;type>/batch.&lt;type. The additional type part in the path
 * is simply there to make the number of path-parts identical with other resources, so relative URLs will still work
 * in CSS files.
 */
public class SuperBatchPluginResource implements DownloadableResource, BatchResource, PluginResource {
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "superbatch" + PATH_SEPARATOR;
    static final String DEFAULT_RESOURCE_NAME_PREFIX = "batch";

    private final BatchPluginResource delegate;
    private final String resourceName;

    public static boolean matches(String path) {
        String type = getType(path);
        return path.indexOf(URL_PREFIX) != -1 && endsWith(path, DEFAULT_RESOURCE_NAME_PREFIX, ".", type);
    }

    public static SuperBatchPluginResource createBatchFor(PluginResource pluginResource) {
        return new SuperBatchPluginResource(getType(pluginResource.getResourceName()), pluginResource.getParams());
    }

    public static SuperBatchPluginResource parse(String path, Map<String, String> params) {
        String type = path.substring(path.lastIndexOf(".") + 1);
        return new SuperBatchPluginResource(type, params);
    }

    protected static String getType(String path) {
        int index = path.lastIndexOf('.');
        if (index > -1 && index < path.length())
            return path.substring(index + 1);

        return "";
    }

    public SuperBatchPluginResource(String type, Map<String, String> params) {
        this(DEFAULT_RESOURCE_NAME_PREFIX + "." + type, type, params);
    }

    protected SuperBatchPluginResource(String resourceName, String type, Map<String, String> params) {
        this.resourceName = resourceName;
        this.delegate = new BatchPluginResource(null, type, params);
    }

    public boolean isResourceModified(HttpServletRequest request, HttpServletResponse response) {
        return delegate.isResourceModified(request, response);
    }

    public void serveResource(HttpServletRequest request, HttpServletResponse response) throws DownloadException {
        delegate.serveResource(request, response);
    }

    public void streamResource(OutputStream out) throws DownloadException {
        delegate.streamResource(out);
    }

    public String getContentType() {
        return delegate.getContentType();
    }

    public void add(DownloadableResource downloadableResource) {
        delegate.add(downloadableResource);
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public String getUrl() {
        StringBuilder buf = new StringBuilder(URL_PREFIX.length() + 20);
        buf.append(URL_PREFIX).append(getType()).append(PATH_SEPARATOR).append(resourceName);
        delegate.addParamsToUrl(buf, delegate.getParams());
        return buf.toString();
    }

    public Map<String, String> getParams() {
        return delegate.getParams();
    }

    public String getVersion(WebResourceIntegration integration) {
        return integration.getSuperBatchVersion();
    }

    public String getType() {
        return delegate.getType();
    }

    public boolean isCacheSupported() {
        return true;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getModuleCompleteKey() {
        return "superbatch";
    }

    @Override
    public String toString() {
        return "[Superbatch name=" + resourceName + ", type=" + getType() + ", params=" + getParams() + "]";
    }
}
