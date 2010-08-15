package org.maera.plugin.osgi;


public class DefaultBooleanFlag implements BooleanFlag {
    private volatile boolean flag;

    public DefaultBooleanFlag(boolean initialValue) {
        this.flag = initialValue;
    }

    public DefaultBooleanFlag() {
        this(false);
    }

    public boolean get() {
        return flag;
    }

    public void set(boolean flag) {
        this.flag = flag;
    }
}
