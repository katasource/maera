package org.maera.plugin.webresource;

import org.maera.plugin.Plugin;
import org.maera.plugin.servlet.DownloadException;
import org.maera.plugin.servlet.DownloadableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.maera.plugin.servlet.AbstractFileServerServlet.PATH_SEPARATOR;
import static org.maera.plugin.servlet.AbstractFileServerServlet.SERVLET_PATH;

/**
 * Represents a batch of plugin resources. <p/>
 * <p/>
 * It provides methods to parse and generate urls to locate a batch of plugin resources. <p/>
 * <p/>
 * Note BatchPluginResource is also a type of {@link DownloadableResource}. The underlying implementation simply
 * keeps a list of {@link DownloadableResource} of which this batch represents and delegates method calls.
 *
 * @since 2.2
 */
public class BatchPluginResource implements DownloadableResource, PluginResource, BatchResource {
    private static final Logger log = LoggerFactory.getLogger(BatchPluginResource.class);

    /**
     * The url prefix for a batch of plugin resources: "/download/batch/"
     */
    static final String URL_PREFIX = PATH_SEPARATOR + SERVLET_PATH + PATH_SEPARATOR + "batch";

    final private String type;
    final private String moduleCompleteKey;
    final private Map<String, String> params;
    final private String resourceName;
    final private List<DownloadableResource> resources;

    /**
     * A constructor that creates a default resource name for the batch in the format: moduleCompleteKey.type
     * For example: test.plugin:resources.js
     * <p/>
     * Note that name of the batch does not identify what the batch includes and could have been static e.g. batch.js
     */
    public BatchPluginResource(final String moduleCompleteKey, final String type, final Map<String, String> params) {
        this(moduleCompleteKey + "." + type, moduleCompleteKey, type, params);
    }

    /**
     * This constructor should only ever be used internally within this class. It does not ensure that the resourceName's
     * file extension is the same as the given type. It is up to the calling code to ensure this.
     */
    private BatchPluginResource(final String resourceName, final String moduleCompleteKey, final String type, final Map<String, String> params) {
        this.resourceName = resourceName;
        this.moduleCompleteKey = moduleCompleteKey;
        this.type = type;
        this.params = params;
        resources = new ArrayList<DownloadableResource>();
    }

    /**
     * @return true if there are no resources included in this batch
     */
    public boolean isEmpty() {
        return resources.isEmpty();
    }

    public void add(final DownloadableResource resource) {
        resources.add(resource);
    }

    public boolean isResourceModified(final HttpServletRequest request, final HttpServletResponse response) {
        for (final DownloadableResource resource : resources) {
            if (resource.isResourceModified(request, response)) {
                return true;
            }
        }
        return false;
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException {
        log.debug("Start to serve batch " + toString());
        for (final DownloadableResource resource : resources) {
            resource.serveResource(request, response);
            writeNewLine(response);
        }
    }

    public void streamResource(final OutputStream out) throws DownloadException {
        for (final DownloadableResource resource : resources) {
            resource.streamResource(out);
            writeNewLine(out);
        }
    }

    /**
     * If a minified files follows another file and the former does not have a free floating carriage return AND ends in
     * a // comment then the one line minified file will in fact be lost from view in a batched send.  So we need
     * to put a new line between files
     *
     * @param response the HTTP response
     * @throws org.maera.plugin.servlet.DownloadException
     *          wraps an IOException (probably client abort)
     */
    private void writeNewLine(final HttpServletResponse response) throws DownloadException {
        try {
            writeNewLine(response.getOutputStream());
        }
        catch (final IOException e) {
            throw new DownloadException(e);
        }
    }

    private void writeNewLine(final OutputStream out) throws DownloadException {
        try {
            out.write('\n');
        }
        catch (final IOException e) {
            throw new DownloadException(e);
        }
    }

    public String getContentType() {
        final String contentType = params.get("content-type");
        if (contentType != null) {
            return contentType;
        }
        return null;
    }

    /**
     * Parses the given url and query parameter map into a BatchPluginResource. Query paramters must be
     * passed in through the map, any in the url String will be ignored.
     *
     * @param url         the url to parse
     * @param queryParams a map of String key and value pairs representing the query parameters in the url
     * @return the parsed BatchPluginResource
     * @throws UrlParseException if the url passed in is not a valid batch resource url
     */
    public static BatchPluginResource parse(String url, final Map<String, String> queryParams) throws UrlParseException {
        final int startIndex = url.indexOf(URL_PREFIX) + URL_PREFIX.length() + 1;

        if (url.indexOf('?') != -1) // remove query parameters
        {
            url = url.substring(0, url.indexOf('?'));
        }

        final String typeAndModuleKey = url.substring(startIndex);
        final String[] parts = typeAndModuleKey.split("/", 2);

        if (parts.length < 2) {
            throw new UrlParseException("Could not parse invalid batch resource url: " + url);
        }

        final String moduleKey = parts[0];
        final String resourceName = parts[1];
        final String type = resourceName.substring(resourceName.lastIndexOf('.') + 1);

        return new BatchPluginResource(resourceName, moduleKey, type, queryParams);
    }

    public static boolean matches(final String url) {
        return url.indexOf(URL_PREFIX) != -1;
    }

    /**
     * Returns a url string in the format: /download/batch/MODULE_COMPLETE_KEY/resourceName?PARAMS
     * <p/>
     * e.g. /download/batch/example.plugin:webresources/example.plugin:webresources.css?ie=true
     * <p/>
     * It is important for the url structure to be:
     * 1. the same number of sectioned paths as the SinglePluginResource
     * 2. include the module completey key in the path before the resource name
     * This is due to css resources referencing other resources such as images in relative path forms.
     */
    public String getUrl() {
        final StringBuilder sb = new StringBuilder();
        sb.append(URL_PREFIX).append(PATH_SEPARATOR).append(moduleCompleteKey).append(PATH_SEPARATOR).append(resourceName);

        addParamsToUrl(sb, params);

        return sb.toString();
    }

    protected void addParamsToUrl(StringBuilder sb, Map<String, String> params) {
        if (params.size() > 0) {
            sb.append("?");
            int count = 0;

            for (final Map.Entry<String, String> entry : params.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue());

                if (++count < params.size()) {
                    sb.append("&");
                }
            }
        }
    }

    public String getResourceName() {
        return resourceName;
    }

    public Map<String, String> getParams() {
        return Collections.unmodifiableMap(params);
    }

    public String getVersion(WebResourceIntegration integration) {
        final Plugin plugin = integration.getPluginAccessor().getEnabledPluginModule(getModuleCompleteKey()).getPlugin();
        return plugin.getPluginInformation().getVersion();
    }

    public String getModuleCompleteKey() {
        return moduleCompleteKey;
    }

    public boolean isCacheSupported() {
        return !"false".equals(params.get("cache"));
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        final BatchPluginResource that = (BatchPluginResource) o;

        if (moduleCompleteKey != null ? !moduleCompleteKey.equals(that.moduleCompleteKey) : that.moduleCompleteKey != null) {
            return false;
        }
        if (params != null ? !params.equals(that.params) : that.params != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (moduleCompleteKey != null ? moduleCompleteKey.hashCode() : 0);
        result = 31 * result + (params != null ? params.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "[moduleCompleteKey=" + moduleCompleteKey + ", type=" + type + ", params=" + params + "]";
    }
}
