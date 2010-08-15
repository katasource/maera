package org.maera.plugin.webresource;

import java.util.Arrays;
import java.util.List;

/**
 *
 */
class TestResourceBatchingConfiguration implements ResourceBatchingConfiguration {
    public boolean enabled = false;

    public boolean isSuperBatchingEnabled() {
        return enabled;
    }

    public List<String> getSuperBatchModuleCompleteKeys() {
        return Arrays.asList(
                "test.atlassian:superbatch",
                "test.atlassian:superbatch2",
                "test.atlassian:missing-plugin"
        );
    }
}
