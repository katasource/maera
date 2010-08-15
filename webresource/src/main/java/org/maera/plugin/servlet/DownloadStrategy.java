package org.maera.plugin.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface DownloadStrategy {
    /**
     * Returns true if the DownloadStrategy is supported for the given url path.
     *
     * @param requestUri the result of {@link HttpServletRequest#getRequestURI()} converted to lowercase
     */
    boolean matches(String requestUri);

    /**
     * Serves the file for the given request and response.
     *
     * @throws DownloadException if there was an error during serving of the file.
     */
    void serveFile(HttpServletRequest request, HttpServletResponse response) throws DownloadException;
}