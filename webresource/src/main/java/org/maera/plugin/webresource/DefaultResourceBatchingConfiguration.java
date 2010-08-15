package org.maera.plugin.webresource;

import java.util.Collections;
import java.util.List;

/**
 * Default configuration for the plugin resource locator, for those applications that do not want to perform
 * any super-batching.
 */
public class DefaultResourceBatchingConfiguration implements ResourceBatchingConfiguration {
    public boolean isSuperBatchingEnabled() {
        return false;
    }

    public List<String> getSuperBatchModuleCompleteKeys() {
        return Collections.emptyList();
    }
}
