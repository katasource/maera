package org.maera.plugin.event.listeners;

import junit.framework.Assert;

public class FailListener {
    private final Class<?> clazz;

    public FailListener(final Class<?> clazz) {
        this.clazz = clazz;
    }

    public void channel(final Object o) {
        if (clazz.isInstance(o)) {
            Assert.fail("Event thrown of type " + clazz.getName());
        }
    }
}
