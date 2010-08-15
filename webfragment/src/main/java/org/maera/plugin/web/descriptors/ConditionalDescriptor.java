package org.maera.plugin.web.descriptors;

import org.maera.plugin.web.Condition;

/**
 * A simple interface implemented by any descriptors that support display
 * conditions.
 *
 * @since 2.5.0
 */
public interface ConditionalDescriptor {
    Condition getCondition();
}
