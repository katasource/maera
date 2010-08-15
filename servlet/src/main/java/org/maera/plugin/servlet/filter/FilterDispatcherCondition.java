package org.maera.plugin.servlet.filter;

/**
 * The dispatching conditions that are taken into account when deciding to match a filter.  These match to the dispatcher
 * values allowed in the Servlet API version 2.4.
 *
 * @since 2.5.0
 */
public enum FilterDispatcherCondition {
    REQUEST,
    INCLUDE,
    FORWARD,
    ERROR;

    /**
     * Determines if a dispatcher value is a valid condition
     *
     * @param dispatcher The dispatcher value.  Null allowed.
     * @return True if valid, false otherwise
     */
    public static boolean contains(String dispatcher) {
        for (FilterDispatcherCondition cond : values()) {
            if (cond.toString().equals(dispatcher)) {
                return true;
            }
        }
        return false;
    }
}
