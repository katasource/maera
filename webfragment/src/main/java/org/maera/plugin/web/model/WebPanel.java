package org.maera.plugin.web.model;

import java.util.Map;

/**
 * The module that is responsive for providing the raw content for a Web Panel.
 * Whatever is returned by {@link #getHtml(java.util.Map)} is inserted into the
 * host application's page, so it has to be valid HTML.
 *
 * @see org.maera.plugin.web.descriptors.DefaultWebPanelModuleDescriptor#getModule()
 * @since 2.5.0
 */
public interface WebPanel {
    /**
     * Returns the HTML that will be placed in the host application's page.
     *
     * @param context the contextual information that can be used during
     *                rendering. Context elements are not standardized and are
     *                application-specific, so refer to your application's documentation to
     *                learn what is available.
     * @return the HTML that will be placed in the host application's page.
     */
    String getHtml(Map<String, Object> context);
}
