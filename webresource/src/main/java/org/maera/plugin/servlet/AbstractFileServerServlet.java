package org.maera.plugin.servlet;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public abstract class AbstractFileServerServlet extends HttpServlet {
    public static final String PATH_SEPARATOR = "/";
    public static final String RESOURCE_URL_PREFIX = "resources";
    public static final String SERVLET_PATH = "download";
    private static final Logger log = LoggerFactory.getLogger(AbstractFileServerServlet.class);

    @Override
    protected final void doGet(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws IOException {
        final DownloadStrategy downloadStrategy = getDownloadStrategy(httpServletRequest);
        if (downloadStrategy == null) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "The file you were looking for was not found");
            return;
        }

        try {
            downloadStrategy.serveFile(httpServletRequest, httpServletResponse);
        }
        catch (final DownloadException e) {
            log.debug("Error while serving file for request:" + httpServletRequest.getRequestURI(), e);
            if (!httpServletResponse.isCommitted()) {
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while serving file");
            }
        }
    }

    /**
     * Returns a list of {@link DownloadStrategy} objects in the order that they will be matched against.
     * The list returned should be cached as this method is called for every request.
     */
    protected abstract List<DownloadStrategy> getDownloadStrategies();

    private DownloadStrategy getDownloadStrategy(final HttpServletRequest httpServletRequest) {
        final String url = httpServletRequest.getRequestURI();
        DownloadStrategy strategy = findStrategy(url);

        if (strategy == null) {
            strategy = findStrategy(url.toLowerCase());
        }

        return strategy;
    }

    private DownloadStrategy findStrategy(String url) {
        for (final DownloadStrategy downloadStrategy : getDownloadStrategies()) {
            if (downloadStrategy.matches(url)) {
                return downloadStrategy;
            }
        }
        return null;
    }
}
