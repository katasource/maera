package org.maera.plugin.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.maera.plugin.Plugin;
import org.maera.plugin.elements.ResourceLocation;
import org.maera.plugin.servlet.util.LastModifiedHandler;
import org.maera.plugin.util.PluginUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * This base class is used to provide the ability to server minified versions of
 * files if required and available.
 *
 * @since 2.2
 */
abstract class AbstractDownloadableResource implements DownloadableResource {
    private static final Logger log = LoggerFactory.getLogger(AbstractDownloadableResource.class);

    /**
     * This is a the system environment variable to set to disable the
     * minification naming strategy used to find web resources.
     */
    private static final String MAERA_WEBRESOURCE_DISABLE_MINIFICATION = "maera.webresource.disable.minification";

    /* the following protected fields are marked final since 2.5 */

    protected final Plugin plugin;
    protected final String extraPath;
    protected final ResourceLocation resourceLocation;

    // PLUG-538 cache this so we don't recreate the string every time it is
    // called
    private final String location;
    private final boolean disableMinification;

    public AbstractDownloadableResource(final Plugin plugin, final ResourceLocation resourceLocation, final String extraPath) {
        this(plugin, resourceLocation, extraPath, false);
    }

    public AbstractDownloadableResource(final Plugin plugin, final ResourceLocation resourceLocation, String extraPath, final boolean disableMinification) {
        if ((extraPath != null) && !"".equals(extraPath.trim()) && !resourceLocation.getLocation().endsWith("/")) {
            extraPath = "/" + extraPath;
        }
        this.disableMinification = disableMinification;
        this.plugin = plugin;
        this.extraPath = extraPath;
        this.resourceLocation = resourceLocation;
        this.location = resourceLocation.getLocation() + extraPath;
    }

    public void serveResource(final HttpServletRequest request, final HttpServletResponse response) throws DownloadException {
        if (log.isDebugEnabled()) {
            log.debug("Serving: " + this);
        }

        final InputStream resourceStream = getResourceAsStreamViaMinificationStrategy();
        if (resourceStream == null) {
            log.warn("Resource not found: " + this);
            return;
        }

        final String contentType = getContentType();
        if (StringUtils.isNotBlank(contentType)) {
            response.setContentType(contentType);
        }

        OutputStream out;
        try {
            out = response.getOutputStream();
        }
        catch (final IOException e) {
            throw new DownloadException(e);
        }

        streamResource(resourceStream, out);
        log.debug("Serving file done.");
    }

    public void streamResource(final OutputStream out) throws DownloadException {
        final InputStream resourceStream = getResourceAsStreamViaMinificationStrategy();
        if (resourceStream == null) {
            log.warn("Resource not found: " + this);
            return;
        }

        streamResource(resourceStream, out);
    }

    /**
     * Copy from the supplied OutputStream to the supplied InputStream. Note
     * that the InputStream will be closed on completion.
     *
     * @param in  the stream to read from
     * @param out the stream to write to
     * @throws DownloadException if an IOException is encountered writing to the
     *                           out stream
     */
    private void streamResource(final InputStream in, final OutputStream out) throws DownloadException {
        try {
            IOUtils.copy(in, out);
        }
        catch (final IOException e) {
            throw new DownloadException(e);
        }
        finally {
            IOUtils.closeQuietly(in);
            try {
                out.flush();
            }
            catch (final IOException e) {
                log.debug("Error flushing output stream", e);
            }
        }
    }

    /**
     * Checks any "If-Modified-Since" header from the request against the
     * plugin's loading time, since plugins can't be modified after they've been
     * loaded this is a good way to determine if a plugin resource has been
     * modified or not. If this method returns true, don't do any more
     * processing on the request -- the response code has already been set to
     * "304 Not Modified" for you, and you don't need to serve the file.
     */
    public boolean isResourceModified(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) {
        final Date resourceLastModifiedDate = (plugin.getDateLoaded() == null) ? new Date() : plugin.getDateLoaded();
        final LastModifiedHandler lastModifiedHandler = new LastModifiedHandler(resourceLastModifiedDate);
        return lastModifiedHandler.checkRequest(httpServletRequest, httpServletResponse);
    }

    public String getContentType() {
        return resourceLocation.getContentType();
    }

    /**
     * Returns an {@link InputStream} to stream the resource from based on
     * resource name.
     *
     * @param resourceLocation the location of the resource to try and load
     * @return an InputStream if the resource can be found or null if cant be
     *         found
     */
    protected abstract InputStream getResourceAsStream(String resourceLocation);

    /**
     * This is called to return the location of the resource that this object
     * represents.
     *
     * @return the location of the resource that this object represents.
     */
    protected String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "Resource: " + getLocation() + " (" + getContentType() + ")";
    }

    /**
     * This is called to use a minification naming strategy to find resources.
     * If a minified file cant by found then the base location is ised as the
     * fall back
     *
     * @return an InputStream r null if nothing can be found for the resource
     *         name
     */
    private InputStream getResourceAsStreamViaMinificationStrategy() {

        InputStream inputStream = null;
        final String location = getLocation();
        if (minificationStrategyInPlay(location)) {
            final String minifiedLocation = getMinifiedLocation(location);
            inputStream = getResourceAsStream(minifiedLocation);
        }
        if (inputStream == null) {
            inputStream = getResourceAsStream(location);
        }
        return inputStream;
    }

    /**
     * Returns true if the minification strategy should be applied to a given
     * resource name
     *
     * @param resourceLocation the location of the resource
     * @return true if the minification strategy should be used.
     */
    private boolean minificationStrategyInPlay(final String resourceLocation) {
        // check if minification has been turned off for this resource (at the
        // module level)
        if (disableMinification) {
            return false;
        }

        // secondly CHECK if we have a System property set to true that DISABLES
        // the minification
        try {
            if (Boolean.getBoolean(MAERA_WEBRESOURCE_DISABLE_MINIFICATION) || Boolean.getBoolean(PluginUtils.MAERA_DEV_MODE)) {
                return false;
            }
        }
        catch (final SecurityException se) {
            // some app servers might have protected access to system
            // properties. Unlikely but lets be defensive
        }
        // We only minify .js or .css files
        if (resourceLocation.endsWith(".js")) {
            // Check if it is already the minified vesrion of the file
            return !(resourceLocation.endsWith("-min.js") || resourceLocation.endsWith(".min.js"));
        }
        if (resourceLocation.endsWith(".css")) {
            // Check if it is already the minified vesrion of the file
            return !(resourceLocation.endsWith("-min.css") || resourceLocation.endsWith(".min.css"));
        }
        // Not .js or .css, don't bother trying to find a minified version (may
        // save some file operations)
        return false;
    }

    private String getMinifiedLocation(final String location) {
        final int lastDot = location.lastIndexOf(".");
        // this can never but -1 since the method call is protected by a call to
        // minificationStrategyInPlay() first
        return location.substring(0, lastDot) + "-min" + location.substring(lastDot);
    }
}
