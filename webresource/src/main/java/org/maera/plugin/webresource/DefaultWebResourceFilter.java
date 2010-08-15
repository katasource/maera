package org.maera.plugin.webresource;

/**
 * A Web Resource Filter that allows for css and javascript resources.
 * <p/>
 * This is the default filter used by the {@link WebResourceManagerImpl} for include/get resource methods that do
 * not accept a filter as a parameter.
 *
 * @since 2.4
 */
public class DefaultWebResourceFilter implements WebResourceFilter {
    public static final DefaultWebResourceFilter INSTANCE = new DefaultWebResourceFilter();

    public boolean matches(String resourceName) {
        return JavascriptWebResource.FORMATTER.matches(resourceName) || CssWebResource.FORMATTER.matches(resourceName);
    }
}
