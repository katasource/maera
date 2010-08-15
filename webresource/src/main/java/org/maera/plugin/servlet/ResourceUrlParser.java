package org.maera.plugin.servlet;

/**
 * Parses resource download URLs for a plugin resource download strategy, and can determine whether a given URL is valid
 * for that download strategy.
 * <p/>
 * The URLs are in the form "/servletPath/strategyPrefix/moduleCompleteKey/resourceName", where the 'servletPath' is
 * defined as {@link AbstractFileServerServlet#SERVLET_PATH}, the 'strategyPrefix' is defined by a constructor parameter,
 * the remainder is parsed by this parser into a {@link PluginResource}.
 *
 * @deprecated Since 2.2
 */
public class ResourceUrlParser {
    private final String strategyPrefix;

    /**
     * Create a parser with the given strategy prefix for parsing URLs.
     * <p/>
     * For example, a strategy prefix of 'resources' means that this parser will match URLs which include
     * 'download/resources'. (Where 'download' is defined as {@link AbstractFileServerServlet#SERVLET_PATH}.)
     *
     * @param strategyPrefix a String which will be found following 'download' at the start of matching URLs.
     */
    public ResourceUrlParser(String strategyPrefix) {
        this.strategyPrefix = strategyPrefix;
    }

    /**
     * Parses resource download URLs for this plugin resource download strategy, returning a {@link PluginResource}.
     * Returns <tt>null</tt> if the URL does not match (i.e. {@link #matches(String)} returns <tt>false</tt>), or the
     * final part of the URL cannot be split into a moduleCompleteKey and resourceName (i.e. there's no slash in the
     * final component).
     * <p/>
     * Parsing uses the download strategy prefix to determine where in the URL string to start. The strategy prefix and
     * one character following it (typically a slash) is skipped, then the remainder is split on the first slash found.
     *
     * @param resourceUrl the URL of the resource request
     * @return a resource which includes the plugin or plugin module's complete key and the resource name, or
     *         <tt>null</tt> if the URL doesn't parse correctly.
     */
    public PluginResource parse(String resourceUrl) {
        if (!matches(resourceUrl))
            return null;

        int indexOfStrategyPrefix = resourceUrl.indexOf(strategyPrefix);
        String libraryAndResource = resourceUrl.substring(indexOfStrategyPrefix + strategyPrefix.length() + 1);
        String[] parts = libraryAndResource.split("/", 2);

        if (parts.length != 2)
            return null;
        return new PluginResource(parts[0], parts[1]);
    }

    /**
     * Returns true if the provided URL matches a the URL prefix defined for this download strategy.
     *
     * @param resourceUrl the URL of the resource request
     * @return <tt>true</tt> if the URL designates a request for this download strategy, otherwise <tt>false</tt>.
     */
    public boolean matches(String resourceUrl) {
        return resourceUrl.indexOf(AbstractFileServerServlet.SERVLET_PATH + "/" + strategyPrefix) != -1;
    }
}
