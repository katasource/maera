package org.maera.plugin.osgi;

public class HostClassUsingHostComponentSetter {
    private SomeInterface someInterface;

    public HostClassUsingHostComponentSetter() {
    }

    public void setSomeInterface(SomeInterface someInterface) {
        this.someInterface = someInterface;
    }

    public SomeInterface getSomeInterface() {
        return someInterface;
    }
}
