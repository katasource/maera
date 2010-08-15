package org.maera.plugin.web.model;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Represents a plain text, primarily used as a links name
 */
public interface WebLabel extends WebParam {
    String getKey();

    String getNoKeyValue();

    String getDisplayableLabel(HttpServletRequest req, Map<String, Object> context);
}
