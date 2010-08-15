package org.maera.plugin.elements;

import java.util.Map;

/**
 * This class gives the location of a particular resource
 */
public class ResourceLocation {
    private String location;
    private String name;
    private String type;
    private String contentType;
    private String content;
    private Map<String, String> params;

    public ResourceLocation(String location, String name, String type, String contentType, String content, Map<String, String> params) {
        this.location = location;
        this.name = name;
        this.type = type;
        this.contentType = contentType;
        this.content = content;
        this.params = params;
    }

    public String getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContent() {
        return content;
    }

    public String getParameter(String key) {
        return params.get(key);
    }
}
