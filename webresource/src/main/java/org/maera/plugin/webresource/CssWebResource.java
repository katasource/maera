package org.maera.plugin.webresource;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CssWebResource extends AbstractWebResourceFormatter {
    static final WebResourceFormatter FORMATTER = new CssWebResource();

    private static final String CSS_EXTENSION = ".css";
    private static final String MEDIA_PARAM = "media";
    private static final String IEONLY_PARAM = "ieonly";

    private static final List<String> HANDLED_PARAMETERS = Arrays.asList("title", MEDIA_PARAM, "charset");

    public boolean matches(String name) {
        return name != null && name.endsWith(CSS_EXTENSION);
    }

    public String formatResource(String url, Map<String, String> params) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"").append(url).append("\"");
        List attributes = getParametersAsAttributes(params);
        if (attributes != null && attributes.size() > 0) {
            buffer.append(" ").append(StringUtils.join(attributes.iterator(), " "));
        }

        // default media to all
        if (!params.containsKey(MEDIA_PARAM)) {
            buffer.append(" media=\"all\"");
        }
        buffer.append(">\n");

        // ie conditional commment
        if (BooleanUtils.toBoolean(params.get(IEONLY_PARAM))) {
            buffer.insert(0, "<!--[if IE]>\n");
            buffer.append("<![endif]-->\n");
        }

        return buffer.toString();
    }

    protected List<String> getAttributeParameters() {
        return HANDLED_PARAMETERS;
    }
}
