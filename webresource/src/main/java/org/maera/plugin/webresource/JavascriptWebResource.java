package org.maera.plugin.webresource;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class JavascriptWebResource extends AbstractWebResourceFormatter {
    static final WebResourceFormatter FORMATTER = new JavascriptWebResource();

    private static final String JAVA_SCRIPT_EXTENSION = ".js";
    private static final List<String> HANDLED_PARAMETERS = Arrays.asList("charset");

    public boolean matches(String name) {
        return name != null && name.endsWith(JAVA_SCRIPT_EXTENSION);
    }

    public String formatResource(String url, Map<String, String> params) {
        StringBuffer buffer = new StringBuffer("<script type=\"text/javascript\" ");
        buffer.append("src=\"").append(url).append("\" ");
        buffer.append(StringUtils.join(getParametersAsAttributes(params).iterator(), " "));
        buffer.append("></script>\n");
        return buffer.toString();
    }

    protected List<String> getAttributeParameters() {
        return HANDLED_PARAMETERS;
    }
}