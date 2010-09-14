package org.maera.plugin.impl;

import org.maera.plugin.Plugin;

/**
 * @deprecated since 2.2.0, use a wrapping plugin based on {@link AbstractDelegatingPlugin} instead
 */
public interface DynamicPlugin extends Plugin {

    void setDeletable(boolean deletable);


    void setBundled(boolean bundled);

}
