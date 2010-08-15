package org.maera.plugin.web.model;

import org.dom4j.Element;
import org.maera.plugin.web.ContextProvider;
import org.maera.plugin.web.WebFragmentHelper;
import org.maera.plugin.web.descriptors.WebFragmentModuleDescriptor;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single "href", with a variety of permutations.
 */
public class DefaultWebLink extends AbstractWebItem implements WebLink {
    private final String url;
    private final boolean absoluteUrl;
    private final String accessKey;
    private final String id;

    public DefaultWebLink(Element linkEl, WebFragmentHelper webFragmentHelper, ContextProvider contextProvider, WebFragmentModuleDescriptor descriptor) {
        super(webFragmentHelper, contextProvider, descriptor);
        this.url = linkEl.getTextTrim();
        this.accessKey = linkEl.attributeValue("accessKey");
        this.id = linkEl.attributeValue("linkId");
        this.absoluteUrl = "true".equals(linkEl.attributeValue("absolute"));
    }

    public String getRenderedUrl(Map<String, Object> context) {
        Map<String, Object> tmpContext = new HashMap<String, Object>(context);
        tmpContext.putAll(getContextMap(tmpContext));
        return getWebFragmentHelper().renderVelocityFragment(url, tmpContext);
    }

    private boolean isRelativeUrl(String url) {
        return !(absoluteUrl || url.startsWith("http://") || url.startsWith("https://"));
    }

    public String getDisplayableUrl(HttpServletRequest req, Map<String, Object> context) {
        String renderedUrl = getRenderedUrl(context);
        if (isRelativeUrl(renderedUrl))
            return req.getContextPath() + renderedUrl;
        else
            return renderedUrl;
    }

    public boolean hasAccessKey() {
        return accessKey != null && !"".equals(accessKey);
    }

    public String getAccessKey(Map<String, Object> context) {
        context.putAll(getContextMap(context));
        return getWebFragmentHelper().renderVelocityFragment(accessKey, context);
    }

    public String getId() {
        return id;
    }
}
