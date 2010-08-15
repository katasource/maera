package org.maera.plugin.servlet.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * This class manages the last modified date of a single HTTP resource.
 */
public class LastModifiedHandler {
    private long lastModified;
    private String etag;
    private static final int ONE_SECOND_MILLIS = 1000;

    public LastModifiedHandler() {
        modified(new Date());
    }

    public LastModifiedHandler(Date lastModifiedDate) {
        modified(lastModifiedDate);
    }

    /**
     * Check whether we need to generate a response for this request. Set the necessary headers on the response, and if
     * we don't need to provide content, set the response status to 304.
     * <p/>
     * If this method returns true, the caller should not perform any more processing on the request.
     *
     * @return true if we don't need to provide any data to satisfy this request
     */
    public boolean checkRequest(HttpServletRequest request, HttpServletResponse response) {
        return checkRequest(request, response, lastModified, etag);
    }

    /**
     * The content has changed, reset the modified date and the etag
     */
    public void modified() {
        modified(new Date());
    }

    private void modified(Date date) {
        lastModified = calculateLastModifiedDate(date);
        etag = calculateEtag(lastModified);
    }

    private static long calculateLastModifiedDate(Date lastModifiedDate) {
        long lastModified = lastModifiedDate.getTime();
        // resolution of 1 second
        lastModified -= lastModified % ONE_SECOND_MILLIS;
        return lastModified;
    }

    private static String calculateEtag(long lastModified) {
        return "\"" + lastModified + "\"";
    }

    /**
     * This static method is used when the resource being served by the servlet keeps track of the last modified date,
     * and so no state needs to be maintained by this handler.
     */
    public static boolean checkRequest(HttpServletRequest request, HttpServletResponse response, Date lastModifiedDate) {
        long lastModified = calculateLastModifiedDate(lastModifiedDate);
        return checkRequest(request, response, lastModified, calculateEtag(lastModified));
    }

    private static boolean checkRequest(HttpServletRequest request, HttpServletResponse response, long lastModified, String etagString) {
        if ("true".equals(System.getProperty("maera.disable.caches", "false")))
            return false;

        response.setDateHeader("Last-Modified", lastModified);
        response.setHeader("ETag", etagString);

        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (noConditionalGetHeadersFound(ifModifiedSince, ifNoneMatch)
                || isContentModifiedSince(ifModifiedSince, lastModified)
                || !etagMatches(ifNoneMatch, etagString)) {
            return false;
        }
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        return true;
    }

    private static boolean etagMatches(String ifNoneMatch, String etagString) {
        return ifNoneMatch != null && ifNoneMatch.equals(etagString);
    }

    private static boolean isContentModifiedSince(long ifModifiedSince, long lastModified) {
        return ifModifiedSince != -1 && ifModifiedSince < lastModified;
    }

    private static boolean noConditionalGetHeadersFound(long ifModifiedSince, String ifNoneMatch) {
        return ifModifiedSince == -1 && ifNoneMatch == null;
    }
}
