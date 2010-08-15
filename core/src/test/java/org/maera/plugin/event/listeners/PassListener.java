package org.maera.plugin.event.listeners;

import junit.framework.Assert;

public class PassListener {
    private final Class<?> clazz;
    private int called = 0;

    public PassListener(final Class<?> clazz) {
        this.clazz = clazz;
    }

    public void channel(final Object o) {
        if (clazz.isInstance(o)) {
            called++;
        }
    }

    public void assertCalled() {
        Assert.assertTrue("Event not thrown " + clazz.getName(), called > 0);
        reset();
    }

    public void assertCalled(final int times) {
        Assert.assertEquals("Event not thrown " + clazz.getName(), times, called);
        reset();
    }

    public void reset() {
        called = 0;
    }
}
