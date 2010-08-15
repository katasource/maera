package org.maera.plugin.refimpl;

import org.maera.plugin.webresource.UrlMode;

import java.net.URI;

public class ParameterUtils {
    public static String getBaseUrl(UrlMode urlMode) {
        String port = System.getProperty("http.port", "8080");
        String baseUrl = System.getProperty("baseurl", "http://localhost:" + port + "/maera-plugins-refimpl");
        if (urlMode == UrlMode.ABSOLUTE) {
            return baseUrl;
        }
        return URI.create(baseUrl).getPath();
    }
}
