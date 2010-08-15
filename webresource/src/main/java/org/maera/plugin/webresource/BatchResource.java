package org.maera.plugin.webresource;

import java.util.Map;

/**
 * Interface for plugin resources that serve batches.
 */
public interface BatchResource {
    String getType();

    Map<String, String> getParams();
}
